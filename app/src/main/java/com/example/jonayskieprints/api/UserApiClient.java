package com.example.jonayskieprints.api;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.jonayskieprints.model.DashboardStats;
import com.example.jonayskieprints.model.Order;
import com.example.jonayskieprints.model.Prices;
import com.example.jonayskieprints.model.UserProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * UserApiClient — cookie-based auth with MANUAL cookie header injection.
 * Fixes Vercel CORS issue where OkHttp doesn't auto-add cookie headers.
 */
public class UserApiClient {

    private static final String TAG = "UserApiClient";
    private static UserApiClient instance;

    private String baseUrl = "https://jonayskieprints.vercel.app";

    private final List<Cookie> cookieStore = new ArrayList<>();

    private final OkHttpClient http = new OkHttpClient.Builder()
            .cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    Log.d(TAG, "🔄 saveFromResponse called with " + cookies.size() + " cookies");
                    for (Cookie incoming : cookies) {
                        Log.d(TAG, "   ➕ Saving cookie: " + incoming.name() + " = " + incoming.value().substring(0, Math.min(20, incoming.value().length())) + "...");
                        // Remove old version of this cookie
                        cookieStore.removeIf(c -> c.name().equals(incoming.name()));
                        cookieStore.add(incoming);
                    }
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> matching = new ArrayList<>();
                    for (Cookie c : cookieStore) {
                        if (c.matches(url)) {
                            matching.add(c);
                            Log.d(TAG, "   📤 Loading cookie for request: " + c.name());
                        }
                    }
                    return matching;
                }
            })
            .addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws java.io.IOException {
                    Request req = chain.request();
                    Log.d(TAG, "");
                    Log.d(TAG, "╔════════════════════════════════════════════════");
                    Log.d(TAG, "║ REQUEST: " + req.method() + " " + req.url());
                    Log.d(TAG, "║ Cookies header: " + req.headers("Cookie"));

                    Response res = chain.proceed(req);

                    Log.d(TAG, "║ RESPONSE: " + res.code());
                    List<String> setCookieHeaders = res.headers("Set-Cookie");
                    Log.d(TAG, "║ Set-Cookie headers: " + setCookieHeaders.size());
                    for (String setCookie : setCookieHeaders) {
                        Log.d(TAG, "║   " + setCookie.substring(0, Math.min(60, setCookie.length())) + "...");
                    }
                    Log.d(TAG, "╚════════════════════════════════════════════════");
                    Log.d(TAG, "");

                    return res;
                }
            })
            .build();

    private final ExecutorService executor    = Executors.newCachedThreadPool();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    private UserApiClient() {}

    public static UserApiClient get() {
        if (instance == null) instance = new UserApiClient();
        return instance;
    }

    public void setBaseUrl(String url) {
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public OkHttpClient getHttpClient() {
        return http;
    }

    public void clearCookies() {
        cookieStore.clear();
        Log.d(TAG, "🗑️ Cookies cleared");
    }

    public interface PricesCallback  { void onResult(Prices p); }
    public interface StatsCallback   { void onResult(DashboardStats s); }
    public interface OrdersCallback  { void onResult(List<Order> orders); }
    public interface UserCallback    { void onResult(UserProfile user); }
    public interface JsonCallback    { void onResult(JSONObject result); }
    public interface SimpleCallback  { void onResult(boolean ok); }

    // ── POST /api/register ──────────────────────────────────────────────────
    public void register(String firstName, String lastName, String email, String phone, String password, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("first_name", firstName);
                json.put("last_name",  lastName);
                json.put("email",      email);
                json.put("phone",      phone);
                json.put("password",   password);

                RequestBody body = RequestBody.create(
                        json.toString(),
                        okhttp3.MediaType.parse("application/json; charset=utf-8")
                );
                String response = post("/api/register", body);
                boolean ok = new JSONObject(response).optBoolean("success", false);
                mainHandler.post(() -> callback.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "register error", e);
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }

    // ── POST /api/auth/send-otp (Registration OTP) ──────────────────────────
    public void sendRegisterOtp(String firstName, String lastName, String email,
                                 String phone, String password, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("first_name", firstName);
                json.put("last_name",  lastName);
                json.put("email",      email);
                json.put("phone",      phone);
                json.put("password",   password);
                json.put("confirmPassword", password); // backend requires this

                RequestBody body = RequestBody.create(
                        json.toString(),
                        okhttp3.MediaType.parse("application/json; charset=utf-8")
                );
                String response = post("/api/auth/send-otp", body);
                Log.d(TAG, "sendRegisterOtp response: " + response);
                boolean ok = new JSONObject(response).optBoolean("success", false);
                mainHandler.post(() -> callback.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "sendRegisterOtp error", e);
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }

    // ── POST /api/auth/verify-otp (Registration OTP verify + create account) ─
    public void verifyRegisterOtp(String email, String otp, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("otp",   otp);

                RequestBody body = RequestBody.create(
                        json.toString(),
                        okhttp3.MediaType.parse("application/json; charset=utf-8")
                );
                String response = post("/api/auth/verify-otp", body);
                Log.d(TAG, "verifyRegisterOtp response: " + response);
                boolean ok = new JSONObject(response).optBoolean("success", false);
                mainHandler.post(() -> callback.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "verifyRegisterOtp error", e);
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }

    public void login(String email, String password, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email",    email);
                json.put("password", password);

                RequestBody body = RequestBody.create(
                        json.toString(),
                        okhttp3.MediaType.parse("application/json; charset=utf-8")
                );
                String response = post("/api/login", body);
                Log.d(TAG, "✅ login response: " + response);
                boolean ok = new JSONObject(response).optBoolean("success", false);
                mainHandler.post(() -> callback.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "❌ login error", e);
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }

    public void loginWithGoogle(String googleIdToken, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                RequestBody body = new okhttp3.MultipartBody.Builder()
                        .setType(okhttp3.MultipartBody.FORM)
                        .addFormDataPart("id_token", googleIdToken)
                        .build();
                String json = post("/api/auth/google/mobile", body);

                boolean ok = new JSONObject(json).optBoolean("success", false);
                Log.d(TAG, "✅ loginWithGoogle result: " + ok);
                mainHandler.post(() -> callback.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "❌ loginWithGoogle error", e);
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }

    public void logout(SimpleCallback callback) {
        executor.execute(() -> {
            try {
                RequestBody body = new okhttp3.MultipartBody.Builder()
                        .setType(okhttp3.MultipartBody.FORM)
                        .build();
                post("/api/logout", body);
            } catch (Exception ignored) {}
            clearCookies();
            mainHandler.post(() -> { if (callback != null) callback.onResult(true); });
        });
    }

    // ── Forgot Password ──────────────────────────────────────────────────────

    public void forgotPassword(String email, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                RequestBody body = RequestBody.create(
                        json.toString(),
                        okhttp3.MediaType.parse("application/json; charset=utf-8")
                );
                String response = post("/api/forgot-password", body);
                Log.d(TAG, "forgotPassword response: " + response);
                boolean ok = new JSONObject(response).optBoolean("success", false);
                mainHandler.post(() -> callback.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "forgotPassword error", e);
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }

    public void verifyOtp(String email, String otp, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("otp", otp);
                RequestBody body = RequestBody.create(
                        json.toString(),
                        okhttp3.MediaType.parse("application/json; charset=utf-8")
                );
                // ✅ Fixed endpoint: was /api/forgot-password/verify
                String response = post("/api/verify-otp", body);
                Log.d(TAG, "verifyOtp response: " + response);
                boolean ok = new JSONObject(response).optBoolean("success", false);
                mainHandler.post(() -> callback.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "verifyOtp error", e);
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }

    public void resetPassword(String email, String otp, String newPassword, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("newPassword", newPassword);       // ✅ Fixed: was "new_password"
                json.put("confirmPassword", newPassword);   // ✅ Added: backend requires this
                RequestBody body = RequestBody.create(
                        json.toString(),
                        okhttp3.MediaType.parse("application/json; charset=utf-8")
                );
                // ✅ Fixed endpoint: was /api/forgot-password/reset
                String response = post("/api/reset-password", body);
                Log.d(TAG, "resetPassword response: " + response);
                boolean ok = new JSONObject(response).optBoolean("success", false);
                mainHandler.post(() -> callback.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "resetPassword error", e);
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }
    public void fetchStats(StatsCallback callback) {
        executor.execute(() -> {
            try {
                String json = get("/api/dashboard?action=getDashboardStats");
                JSONObject root = new JSONObject(json);
                DashboardStats s = new DashboardStats();
                if (root.optBoolean("success", false)) {
                    JSONObject d = root.getJSONObject("data");
                    s.totalOrders     = d.optInt("totalOrders",     0);
                    s.pendingOrders   = d.optInt("pendingOrders",   0);
                    s.completedOrders = d.optInt("completedOrders", 0);
                    s.totalSpent      = d.optDouble("totalSpent",   0.00);
                }
                mainHandler.post(() -> callback.onResult(s));
            } catch (Exception e) {
                Log.e(TAG, "fetchStats error", e);
                mainHandler.post(() -> callback.onResult(new DashboardStats()));
            }
        });
    }

    public void fetchOrders(String filter, OrdersCallback callback) {
        executor.execute(() -> {
            try {
                String path = "/api/dashboard";
                if (filter != null && !filter.isEmpty()) path += "?status=" + filter;
                String json = get(path);
                JSONObject root = new JSONObject(json);
                List<Order> orders = new ArrayList<>();
                if (root.optBoolean("success", false)) {
                    JSONArray arr = root.getJSONObject("data").getJSONArray("orders");
                    for (int i = 0; i < arr.length(); i++) {
                        orders.add(parseOrder(arr.getJSONObject(i)));
                    }
                }
                mainHandler.post(() -> callback.onResult(orders));
            } catch (Exception e) {
                Log.e(TAG, "fetchOrders error", e);
                mainHandler.post(() -> callback.onResult(new ArrayList<>()));
            }
        });
    }

    public void fetchPrices(PricesCallback callback) {
        executor.execute(() -> {
            try {
                String json = get("/api/pricing");
                JSONObject o = new JSONObject(json);
                Prices p = new Prices();
                p.printBw          = o.optDouble("print_bw",          3);
                p.printColor       = o.optDouble("print_color",       5);
                p.photocopying     = o.optDouble("photocopying",      2);
                p.photoDevelopment = o.optDouble("photo_development", 15);
                p.laminating       = o.optDouble("laminating",        20);
                p.folder           = o.optDouble("folder",            10);
                mainHandler.post(() -> callback.onResult(p));
            } catch (Exception e) {
                Log.e(TAG, "fetchPrices error", e);
                mainHandler.post(() -> callback.onResult(new Prices()));
            }
        });
    }

    public void fetchUser(UserCallback callback) {
        executor.execute(() -> {
            try {
                String json = get("/api/dashboard?action=getUser");
                JSONObject root = new JSONObject(json);
                UserProfile u = new UserProfile();
                if (root.optBoolean("success", false)) {
                    JSONObject d = root.getJSONObject("data");
                    u.firstName = d.optString("first_name");
                    u.lastName  = d.optString("last_name");
                    u.email     = d.optString("email");
                    u.phone     = d.optString("phone");
                    u.role      = d.optString("role", "customer");
                }
                mainHandler.post(() -> callback.onResult(u));
            } catch (Exception e) {
                Log.e(TAG, "fetchUser error", e);
                mainHandler.post(() -> callback.onResult(new UserProfile()));
            }
        });
    }

    public void fetchOrderFiles(String orderId, JsonCallback callback) {
        executor.execute(() -> {
            try {
                String json = get("/api/dashboard?action=getOrder&order_id=" + orderId);
                JSONObject root = new JSONObject(json);
                mainHandler.post(() -> callback.onResult(root));
            } catch (Exception e) {
                Log.e(TAG, "fetchOrderFiles error", e);
                mainHandler.post(() -> callback.onResult(null));
            }
        });
    }

    public void cancelOrder(String orderId, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                RequestBody body = new okhttp3.MultipartBody.Builder()
                        .setType(okhttp3.MultipartBody.FORM)
                        .addFormDataPart("action",   "cancelOrder")
                        .addFormDataPart("order_id", orderId)
                        .build();
                String json = post("/api/dashboard", body);
                boolean ok = new JSONObject(json).optBoolean("success", false);
                mainHandler.post(() -> callback.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "cancelOrder error", e);
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }

    public void placeOrder(RequestBody body, JsonCallback callback) {
        executor.execute(() -> {
            try {
                String json = post("/api/dashboard", body);
                JSONObject result;
                try { result = new JSONObject(json); }
                catch (Exception ex) { result = null; }
                final JSONObject finalResult = result;
                mainHandler.post(() -> callback.onResult(finalResult));
            } catch (Exception e) {
                Log.e(TAG, "❌ placeOrder error", e);
                mainHandler.post(() -> callback.onResult(null));
            }
        });
    }

    public void updateOrder(RequestBody body, JsonCallback callback) {
        placeOrder(body, callback);
    }

    public void updateProfile(RequestBody body, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                String json = post("/api/dashboard", body);
                boolean ok = new JSONObject(json).optBoolean("success", false);
                mainHandler.post(() -> callback.onResult(ok));
            } catch (Exception e) {
                Log.e(TAG, "updateProfile error", e);
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }

    public void uploadFiles(String orderId, List<Uri> files, Context context, SimpleCallback callback) {
        if (callback != null) mainHandler.post(() -> callback.onResult(false));
    }

    // ──────────────────────────────────────────────────────────────────────
    // CORE HTTP METHODS WITH MANUAL COOKIE INJECTION
    // ──────────────────────────────────────────────────────────────────────

    private String get(String path) throws Exception {
        Request.Builder reqBuilder = new Request.Builder()
                .url(baseUrl + path)
                .get();

        // 🔐 MANUALLY ADD COOKIES TO HEADER
        addCookieHeader(reqBuilder, baseUrl + path);

        Request req = reqBuilder.build();
        try (Response r = http.newCall(req).execute()) {
            return r.body() != null ? r.body().string() : "{}";
        }
    }

    private String post(String path, RequestBody body) throws Exception {
        Request.Builder reqBuilder = new Request.Builder()
                .url(baseUrl + path)
                .post(body);

        // 🔐 MANUALLY ADD COOKIES TO HEADER
        addCookieHeader(reqBuilder, baseUrl + path);

        Request req = reqBuilder.build();
        try (Response r = http.newCall(req).execute()) {
            String response = r.body() != null ? r.body().string() : "{}";
            return response;
        }
    }

    /**
     * Manually adds stored cookies to the request header.
     * Fixes issue where OkHttp doesn't auto-serialize cookies on Vercel.
     */
    private void addCookieHeader(Request.Builder reqBuilder, String urlString) {
        try {
            HttpUrl httpUrl = HttpUrl.parse(urlString);
            if (httpUrl == null) return;

            List<Cookie> cookies = http.cookieJar().loadForRequest(httpUrl);
            if (cookies.isEmpty()) return;

            StringBuilder cookieHeader = new StringBuilder();
            for (int i = 0; i < cookies.size(); i++) {
                if (i > 0) cookieHeader.append("; ");
                Cookie c = cookies.get(i);
                cookieHeader.append(c.name()).append("=").append(c.value());
            }

            String cookieHeaderValue = cookieHeader.toString();
            reqBuilder.header("Cookie", cookieHeaderValue);

            Log.d(TAG, "🔐 Manually added Cookie header: " +
                    cookieHeaderValue.substring(0, Math.min(60, cookieHeaderValue.length())) + "...");
        } catch (Exception e) {
            Log.e(TAG, "Error adding cookie header", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────────

    private Order parseOrder(JSONObject o) throws Exception {
        Order ord = new Order();
        ord.orderId        = o.optString("order_id");
        ord.service        = o.optString("service");
        ord.quantity       = o.optInt("quantity", 1);
        ord.status         = o.optString("status", "pending");
        ord.specifications = o.optString("specifications");
        ord.deliveryOption = o.optString("delivery_option");
        ord.pickupTime     = o.optString("pickup_time");
        try { ord.totalAmount = Double.parseDouble(o.optString("total_amount", "0")); }
        catch (Exception ex) { ord.totalAmount = o.optDouble("total_amount", 0); }
        ord.paymentMethod  = o.optString("payment_method");
        ord.createdAt      = o.optString("created_at");
        ord.userName       = o.optString("user_id");
        return ord;
    }
}
