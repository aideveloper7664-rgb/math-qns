import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const SUCCESS_STATUSES = ["SUCCESS", "success", "SUCCESSFUL", "COMPLETED", "CAPTURED", "PAID"];
const FAILURE_STATUSES = ["FAILED", "failed", "CANCELLED", "EXPIRED", "REJECTED", "ERROR"];

serve(async (req) => {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 });

  try {
    const body = await req.json();
    console.log("ZapUPI webhook received:", JSON.stringify(body));

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY);

    const { order_id, status, amount, transaction_id, utr } = body;
    if (!order_id) return new Response("Missing order_id", { status: 400 });

    // Find deposit by gateway_reference
    const { data: dep, error: depErr } = await supabase
      .from("deposits")
      .select("*")
      .eq("gateway_reference", order_id)
      .maybeSingle();

    if (depErr) {
      console.error("DB error fetching deposit:", depErr);
      return new Response("DB error", { status: 500 });
    }
    if (!dep) return new Response("Deposit not found", { status: 404 });

    // Idempotency: if already processed, return OK immediately
    if (dep.status === "SUCCESS") {
      console.log("Already processed:", dep.id);
      return new Response("OK (already processed)", { status: 200 });
    }

    if (SUCCESS_STATUSES.includes(status)) {
      // Mark deposit as successful
      await supabase.from("deposits").update({
        status: "SUCCESS",
        verified_at: new Date().toISOString(),
        metadata: {
          ...(dep.metadata || {}),
          utr: utr || null,
          gateway_txn_id: transaction_id || null,
          raw_webhook: body,
        },
      }).eq("id", dep.id);

      // Credit user's wallet
      const { data: user, error: userErr } = await supabase
        .from("users")
        .select("wallet_balance, total_deposited")
        .eq("id", dep.user_id)
        .single();

      if (userErr || !user) {
        console.error("User not found for deposit:", dep.user_id, userErr);
        return new Response("User not found", { status: 404 });
      }

      const newBalance = Number(user.wallet_balance || 0) + Number(dep.amount);
      const newTotal = Number(user.total_deposited || 0) + Number(dep.amount);

      await supabase.from("users").update({
        wallet_balance: newBalance,
        total_deposited: newTotal,
      }).eq("id", dep.user_id);

      // Insert transaction record
      await supabase.from("transactions").insert({
        user_id: dep.user_id,
        user_name: dep.user_name || "User",
        type: "DEPOSIT",
        amount: Number(dep.amount),
        status: "SUCCESS",
        gateway_or_account: "ZapUPI",
        metadata: {
          deposit_id: dep.id,
          gateway_ref: order_id,
          utr: utr || null,
          transaction_id: transaction_id || null,
        },
      });

      console.log(`✅ Credited ₹${dep.amount} to user ${dep.user_id}`);
      return new Response("OK", { status: 200 });
    } else if (FAILURE_STATUSES.includes(status)) {
      await supabase.from("deposits").update({
        status: "FAILED",
        metadata: {
          ...(dep.metadata || {}),
          failure_reason: body.message || "Payment failed",
          raw_webhook: body,
        },
      }).eq("id", dep.id);

      console.log(`❌ Deposit failed for ${dep.id}: ${body.message || status}`);
      return new Response("OK", { status: 200 });
    } else {
      console.log(`⚠️ Unknown status "${status}" for deposit ${dep.id} — no action taken`);
      return new Response("OK (unknown status, ignored)", { status: 200 });
    }
  } catch (err: any) {
    console.error("Webhook error:", err);
    return new Response(JSON.stringify({ error: err.message }), { status: 500 });
  }
});
