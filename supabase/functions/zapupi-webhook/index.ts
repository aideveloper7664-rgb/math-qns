import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

serve(async (req) => {
  try {
    const body = await req.json();
    console.log("Webhook received:", body);

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY);

    const { order_id, status, amount, transaction_id } = body;
    if (!order_id) return new Response("Missing order_id", { status: 400 });

    // Find deposit by gateway_reference
    const { data: dep } = await supabase
      .from("deposits").select("*").eq("gateway_reference", order_id).maybeSingle();
    if (!dep) return new Response("Deposit not found", { status: 404 });

    // Idempotency: if already processed, return OK immediately
    if (dep.status === "SUCCESS") {
      return new Response("OK (already processed)", { status: 200 });
    }

    if (status === "SUCCESS" || status === "success" || status === "SUCCESSFUL") {
      // Mark deposit as successful
      await supabase.from("deposits").update({
        status: "SUCCESS",
        verified_at: new Date().toISOString(),
        metadata: { ...dep.metadata, webhook_response: body }
      }).eq("id", dep.id);

      // Credit user's wallet atomically
      const { data: user } = await supabase
        .from("users").select("wallet_balance, total_deposited").eq("id", dep.user_id).single();

      if (user) {
        const newBalance = Number(user.wallet_balance || 0) + Number(dep.amount);
        const newTotal = Number(user.total_deposited || 0) + Number(dep.amount);

        await supabase.from("users").update({
          wallet_balance: newBalance,
          total_deposited: newTotal
        }).eq("id", dep.user_id);
      }

      // Transaction record
      await supabase.from("transactions").insert({
        user_id: dep.user_id,
        user_name: dep.user_name || "User",
        type: "DEPOSIT",
        amount: Number(dep.amount),
        status: "SUCCESS",
        gateway_or_account: "ZapUPI",
        metadata: { deposit_id: dep.id, gateway_ref: order_id, transaction_id: transaction_id }
      });

      return new Response("OK", { status: 200 });
    } else {
      // Payment failed
      await supabase.from("deposits").update({
        status: "FAILED",
        metadata: { ...dep.metadata, webhook_response: body }
      }).eq("id", dep.id);

      return new Response("OK", { status: 200 });
    }
  } catch (err: any) {
    console.error("Webhook error:", err);
    return new Response("Error: " + err.message, { status: 500 });
  }
});
