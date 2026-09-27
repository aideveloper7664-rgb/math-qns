import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const SITE_URL = Deno.env.get("SITE_URL") || "https://your-user-panel.netlify.app";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) return json({ error: "Unauthorized" }, 401);

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY);
    const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""));
    if (!user) return json({ error: "Unauthorized" }, 401);

    const { deposit_id, amount } = await req.json();
    if (!deposit_id || !amount || amount < 10) return json({ error: "Invalid input" }, 400);

    // Read payment config (service role bypasses RLS)
    const { data: cfgRow } = await supabase
      .from("game_config").select("value").eq("key", "payment").maybeSingle();

    if (!cfgRow?.value) return json({ error: "Payment not configured" }, 503);

    const cfg = cfgRow.value;
    const gwName = cfg.active_gateway;
    const gw = cfg.gateways?.[gwName];

    if (!gw?.enabled) return json({ error: "Gateway disabled" }, 503);
    if (amount < gw.min_amount || amount > gw.max_amount) {
      return json({ error: "Amount out of range" }, 400);
    }

    // Verify deposit belongs to user
    const { data: dep } = await supabase
      .from("deposits").select("*").eq("id", deposit_id).eq("user_id", user.id).maybeSingle();
    if (!dep) return json({ error: "Deposit not found" }, 404);

    const orderId = `DEP_${deposit_id.replace(/-/g, "").slice(0, 12)}_${Date.now()}`;
    const chargeAmount = cfg.test_mode ? 1 : Number(amount);

    // Call ZapUPI
    const zapRes = await fetch(gw.create_order_url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        zap_key: gw.api_key,
        order_id: orderId,
        amount: chargeAmount,
        customer_mobile: user.phone || "9999999999",
        remark: `Wallet topup for ${user.email}`,
        success_url: `${SITE_URL}/#/wallet?status=success&deposit_id=${deposit_id}`,
        failed_url: `${SITE_URL}/#/wallet?status=failed&deposit_id=${deposit_id}`,
        timeout_url: `${SITE_URL}/#/wallet?status=timeout&deposit_id=${deposit_id}`,
        webhook_url: gw.webhook_url,
      }),
    });

    const zapData = await zapRes.json();
    if (!zapRes.ok) return json({ error: "Gateway error", details: zapData }, 502);

    const checkoutUrl = zapData.checkout_url || zapData.payment_url || zapData.url;

    await supabase.from("deposits").update({
      gateway: gwName,
      gateway_reference: orderId,
      metadata: { ...zapData, test_mode: cfg.test_mode, real_amount: Number(amount) },
    }).eq("id", deposit_id);

    return json({ success: true, order_id: orderId, checkout_url: checkoutUrl });
  } catch (err: any) {
    console.error(err);
    return json({ error: err.message }, 500);
  }
});

function json(data: any, status = 200) {
  return new Response(JSON.stringify(data), {
    status, headers: { ...cors, "Content-Type": "application/json" },
  });
}
