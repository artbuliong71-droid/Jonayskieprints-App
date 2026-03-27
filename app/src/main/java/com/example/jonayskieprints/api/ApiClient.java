package com.example.jonayskieprints.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.jonayskieprints.model.AdminStats;
import com.example.jonayskieprints.model.Customer;
import com.example.jonayskieprints.model.Order;
import com.example.jonayskieprints.model.Pricing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ApiClient wraps every admin API route from the Next.js dashboard.
 *
 * Base URL is set via setBaseUrl() — call this once at app start, e.g.:
 *   ApiClient.get().setBaseUrl("https://your-nextjs-site.vercel.app");
 *
 * ✅ FIX: No longer creates its own OkHttpClient. Instead, it delegates to
 *         UserApiClient.get().getHttpClient() so that the jp_session cookie
 *         set during login is automatically included in every admin request.
 *         Both clients now share one cookie jar — one session, no 401s.
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static ApiClient instance;

    private String baseUrl = "https://jonayskieprints.vercel.app"; // ← CHANGE THIS if needed

    private final ExecutorService executor    = Executors.newCachedThreadPool();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    private ApiClient() {}

    public static synchronized ApiClient get() {
        if (instance == null) instance = new ApiClient();
        return instance;
    }

    public void setBaseUrl(String url) {
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * Returns the shared, cookie-aware OkHttpClient from UserApiClient.
     * This ensures the jp_session cookie set during login is sent with
     * every admin API call — no separate cookie jar needed here.
     */
    private OkHttpClient client() {
        return UserApiClient.get().getHttpClient();
    }

    // ── GET /api/admin/stats ─────────────────────────────────────────────────
    public void fetchStats(Callback<AdminStats> cb) {
        executor.execute(() -> {
            try {
                String json = get("/api/admin/stats");
                JSONObject root = new JSONObject(json);
                if (root.optBoolean("success", false)) {
                    JSONObject d = root.getJSONObject("data");
                    AdminStats s = new AdminStats();
                    s.totalOrders      = d.optInt("totalOrders",      0);
                    s.pendingOrders    = d.optInt("pendingOrders",    0);
                    s.inProgressOrders = d.optInt("inProgressOrders", 0);
                    s.completedOrders  = d.optInt("completedOrders",  0);
                    s.cancelledOrders  = d.optInt("cancelledOrders",  0);
                    s.totalCustomers   = d.optInt("totalCustomers",   0);
                    s.totalSales       = d.optString("totalRevenue",  "0.00");
                    mainHandler.post(() -> cb.onResult(s));
                }
            } catch (Exception e) {
                Log.e(TAG, "fetchStats error", e);
            }
        });
    }

    // ── GET /api/admin/orders?status=xxx ─────────────────────────────────────
    public void fetchOrders(String statusFilter, Callback<List<Order>> cb) {
        executor.execute(() -> {
            try {
                String url = "/api/admin/orders";
                if (statusFilter != null && !statusFilter.isEmpty()) {
                    url += "?status=" + statusFilter;
                }
                String json = get(url);
                JSONObject root = new JSONObject(json);
                List<Order> orders = new ArrayList<>();
                if (root.optBoolean("success", false)) {
                    JSONArray arr = root.getJSONArray("data");
                    for (int i = 0; i < arr.length(); i++) {
                        orders.add(parseOrder(arr.getJSONObject(i)));
                    }
                }
                mainHandler.post(() -> cb.onResult(orders));
            } catch (Exception e) {
                Log.e(TAG, "fetchOrders error", e);
                mainHandler.post(() -> cb.onResult(new ArrayList<>()));
            }
        });
    }

    // ── PATCH /api/admin/orders ───────────────────────────────────────────────
    public void updateOrderStatus(String orderId, String status, Callback<Boolean> cb) {
        executor.execute(() -> {
            try {
                RequestBody body = new FormBody.Builder()
                        .add("order_id", orderId)
                        .add("status",   status)
                        .build();
                String json = patch("/api/admin/orders", body);
                JSONObject root = new JSONObject(json);
                boolean ok = root.optBoolean("success", false);
                mainHandler.post(() -> cb.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "updateOrderStatus error", e);
                mainHandler.post(() -> cb.onResult(false));
            }
        });
    }

    // ── DELETE /api/admin/orders?order_id=xxx ────────────────────────────────
    public void deleteOrder(String orderId, Callback<Boolean> cb) {
        executor.execute(() -> {
            try {
                String json = delete("/api/admin/orders?order_id=" + orderId);
                JSONObject root = new JSONObject(json);
                boolean ok = root.optBoolean("success", false);
                mainHandler.post(() -> cb.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "deleteOrder error", e);
                mainHandler.post(() -> cb.onResult(false));
            }
        });
    }

    // ── GET /api/admin/customers ──────────────────────────────────────────────
    public void fetchCustomers(Callback<List<Customer>> cb) {
        executor.execute(() -> {
            try {
                String json = get("/api/admin/customers");
                JSONObject root = new JSONObject(json);
                List<Customer> list = new ArrayList<>();
                if (root.optBoolean("success", false)) {
                    JSONArray arr = root.getJSONArray("data");
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        Customer c = new Customer();
                        c.id          = o.optString("id");
                        c.firstName   = o.optString("first_name");
                        c.lastName    = o.optString("last_name");
                        c.email       = o.optString("email");
                        c.phone       = o.optString("phone");
                        c.totalOrders = o.optInt("total_orders", 0);
                        c.createdAt   = o.optString("created_at");
                        list.add(c);
                    }
                }
                mainHandler.post(() -> cb.onResult(list));
            } catch (Exception e) {
                Log.e(TAG, "fetchCustomers error", e);
                mainHandler.post(() -> cb.onResult(new ArrayList<>()));
            }
        });
    }

    // ── GET /api/admin/deleted-orders ─────────────────────────────────────────
    public void fetchDeletedOrders(Callback<List<Order>> cb) {
        executor.execute(() -> {
            try {
                String json = get("/api/admin/deleted-orders");
                JSONObject root = new JSONObject(json);
                List<Order> list = new ArrayList<>();
                if (root.optBoolean("success", false)) {
                    JSONArray arr = root.getJSONArray("data");
                    for (int i = 0; i < arr.length(); i++) {
                        list.add(parseOrder(arr.getJSONObject(i)));
                    }
                }
                mainHandler.post(() -> cb.onResult(list));
            } catch (Exception e) {
                Log.e(TAG, "fetchDeletedOrders error", e);
                mainHandler.post(() -> cb.onResult(new ArrayList<>()));
            }
        });
    }

    // ── GET /api/pricing ──────────────────────────────────────────────────────
    public void fetchPricing(Callback<Pricing> cb) {
        executor.execute(() -> {
            try {
                String json = get("/api/pricing");
                JSONObject o = new JSONObject(json);
                Pricing p = new Pricing();
                p.printBw          = o.optDouble("print_bw",          1);
                p.printColor       = o.optDouble("print_color",       2);
                p.photocopying     = o.optDouble("photocopying",      2);
                p.photoDevelopment = o.optDouble("photo_development", 15);
                p.laminating       = o.optDouble("laminating",        20);
                p.folder           = o.optDouble("folder",            10);
                mainHandler.post(() -> cb.onResult(p));
            } catch (Exception e) {
                Log.e(TAG, "fetchPricing error", e);
            }
        });
    }

    // ── POST /api/admin/pricing ───────────────────────────────────────────────
    public void savePricing(Pricing p, Callback<Boolean> cb) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("print_bw",          p.printBw);
                body.put("print_color",       p.printColor);
                body.put("photocopying",      p.photocopying);
                body.put("photo_development", p.photoDevelopment);
                body.put("laminating",        p.laminating);
                body.put("folder",            p.folder);

                okhttp3.MediaType JSON = okhttp3.MediaType.get("application/json; charset=utf-8");
                RequestBody rb = RequestBody.create(body.toString(), JSON);
                String json = postJson("/api/admin/pricing", rb);
                JSONObject root = new JSONObject(json);
                boolean ok = root.optBoolean("success", false);
                mainHandler.post(() -> cb.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "savePricing error", e);
                mainHandler.post(() -> cb.onResult(false));
            }
        });
    }

    // ── POST /api/logout ──────────────────────────────────────────────────────
    public void logout(Callback<Boolean> cb) {
        executor.execute(() -> {
            try {
                RequestBody body = new FormBody.Builder().build();
                post("/api/logout", body);
            } catch (Exception ignored) {}
            mainHandler.post(() -> cb.onResult(true));
        });
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────
    // All helpers call client() so they get the cookie-aware OkHttpClient
    // from UserApiClient — the jp_session cookie is sent automatically.

    private String get(String path) throws IOException {
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .build();
        try (Response r = client().newCall(req).execute()) {
            String body = r.body() != null ? r.body().string() : "{}";
            Log.d(TAG, "GET " + path + " → HTTP " + r.code());
            return body;
        }
    }

    private String post(String path, RequestBody body) throws IOException {
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .post(body)
                .build();
        try (Response r = client().newCall(req).execute()) {
            String resp = r.body() != null ? r.body().string() : "{}";
            Log.d(TAG, "POST " + path + " → HTTP " + r.code());
            return resp;
        }
    }

    private String postJson(String path, RequestBody body) throws IOException {
        return post(path, body);
    }

    private String patch(String path, RequestBody body) throws IOException {
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .patch(body)
                .build();
        try (Response r = client().newCall(req).execute()) {
            String resp = r.body() != null ? r.body().string() : "{}";
            Log.d(TAG, "PATCH " + path + " → HTTP " + r.code());
            return resp;
        }
    }

    private String delete(String path) throws IOException {
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .delete()
                .build();
        try (Response r = client().newCall(req).execute()) {
            String resp = r.body() != null ? r.body().string() : "{}";
            Log.d(TAG, "DELETE " + path + " → HTTP " + r.code());
            return resp;
        }
    }

    // ── Parsers ───────────────────────────────────────────────────────────────

    private Order parseOrder(JSONObject o) throws Exception {
        Order ord = new Order();
        ord.id              = o.optString("_id");
        ord.orderId         = o.optString("order_id");
        ord.userName        = o.optString("user_name");
        ord.userEmail       = o.optString("user_email");
        ord.service         = o.optString("service");
        ord.quantity        = o.optInt("quantity", 1);
        ord.status          = o.optString("status", "pending");
        ord.totalAmount     = o.optDouble("total_amount", 0);
        ord.deliveryOption  = o.optString("delivery_option");
        ord.deliveryAddress = o.optString("delivery_address");
        ord.pickupTime      = o.optString("pickup_time");
        ord.specifications  = o.optString("specifications");
        ord.createdAt       = o.optString("created_at");
        ord.paymentMethod   = o.optString("payment_method");
        ord.gcashRefNum     = o.optString("gcash_ref_num");
        ord.gcashReceiptUrl = o.optString("gcash_receipt_url");

        // Files array
        if (o.has("files")) {
            JSONArray filesArr = o.getJSONArray("files");
            ord.files = new ArrayList<>();
            for (int i = 0; i < filesArr.length(); i++) {
                Order.FileData fd = new Order.FileData();
                Object f = filesArr.get(i);
                if (f instanceof String) {
                    fd.url          = (String) f;
                    fd.resourceType = "image";
                } else {
                    JSONObject fObj = (JSONObject) f;
                    fd.url          = fObj.optString("url");
                    fd.resourceType = fObj.optString("resource_type", "image");
                }
                ord.files.add(fd);
            }
        }
        return ord;
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}