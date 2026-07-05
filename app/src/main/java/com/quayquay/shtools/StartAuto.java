package com.quayquay.shtools;

import static com.quayquay.shtools.extention.ASUtils.delay;
import static com.quayquay.shtools.services.ASBLBridgeService.clearrecents;
import static com.quayquay.shtools.services.ASBLBridgeService.findAndClickByTextDes;
import static com.quayquay.shtools.services.ASBLBridgeService.globalBack;
import static com.quayquay.shtools.services.ASBLBridgeService.globalHome;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.android.gms.tasks.Tasks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.quayquay.hsq.tools.AiBoxApiHelper;
import com.quayquay.hsq.tools.HSQConfig;
import com.quayquay.hsq.tools.HSQDevice;
import com.quayquay.hsq.tools.HSQFileHelper;
import com.quayquay.hsq.tools.HSQHttps;
import com.quayquay.hsq.tools.HSQService;
import com.quayquay.hsq.tools.HSQTools;
import com.quayquay.hsq.tools.HSQTools.TextBlock;
import com.quayquay.hsq.tools.HTMustcApiHelper;
import com.quayquay.hsq.tools.IApiHelper;
import com.quayquay.hsq.tools.NexusMmoApiHelper;
import com.quayquay.hsq.tools.ServerQueuedApiHelper;
import com.quayquay.hsq.tools.TokenRouterApiHelper;
import com.quayquay.shtools.extention.AppInstaller;
import com.quayquay.shtools.screendefinitions.ScreenNode;
import com.quayquay.shtools.services.ASBLBridgeService;

import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StartAuto extends HSQService
{
    private IApiHelper AIHelper;
    private static int widthOfScreen = 0;
    private static int xCenter = 0;
    private static int heightOfScreen = 0;
    private static int yTop = 0;
    private static int yBot = 0;
    private static int yCenter = 0;
    private static int apkVersion = 0;
    private static int remotePromtVersion = 0;
    private String apiRun = "", localServerIp = "", idTelegram = "", customAgentRule = "", textAnswer = "", topText = "", AIWebSite, AIApiKey, aiModel,
            AIWebSite1 = "", AIApiKey1 = "", aiModel1 = "", AIWebSite2 = "", AIApiKey2 = "", aiModel2 = "";
    private boolean AIProxyEnabled = false;
    private String AIProxyUrl = "https://quaykute.id.vn";

    private static final int VCode = BuildConfig.VERSION_CODE;
    private static final int MAX_LOCAL_BLIND_IMAGES = 8;
    private static final int MAX_SURVEY_BRIEF_CHARS = 1400;
    private static final String API_PREFS = "QQ_PREFS_DATA";
    private static final String RUNTIME_PREFS = "QQ_PREFS";
    private static final String PREF_SAVED_API_KEY = "SAVED_API_KEY";
    private static final String PREF_SAVED_DEVICE_ID = "saved_device_id";
    private static final String PREF_RESTART_FROM_VOLUME = "restart_from_volume";
    private static final String PREF_SUPPRESS_AUTOSTART_AFTER_VOLUME = "suppress_autostart_after_volume";
    private static final String PREF_SUPPRESS_ASBL_AUTO_OPEN_UNTIL = "suppress_asbl_auto_open_until";
    private static final int VOLUME_RESTART_REQUEST_CODE = 20260703;
    private static final long ASBL_AUTO_OPEN_SUPPRESS_MS = 10000L;
    private static final String SURVEY_BRIEF_PREFS = "QQ_SURVEY_BRIEF";
    private static final String SURVEY_BRIEF_KEY_TEXT = "survey_brief_text";
    private static final String SURVEY_BRIEF_KEY_FP = "survey_brief_fp";
    public static String deviceID = "UNKNOWN";
    private static String shortDeviceID = getShortDeviceID();
    @SuppressLint("SdCardPath")
    private final String imagePath = "/sdcard/Pictures/ImageChat";
    private String pathInfoProfileSaved;
    private String surveyBriefMemory = "";
    private String surveyBriefFingerprint = "";
    private android.graphics.Rect activeDropdownSwipeBounds = null;

    private static boolean isValidDeviceId(String value)
    {
        if (value == null) return false;
        String clean = value.trim();
        return !clean.isEmpty() && !"UNKNOWN".equalsIgnoreCase(clean);
    }

    private String resolveDeviceId(JSONObject object, Context context, SharedPreferences runtimePrefs)
    {
        String fromIntent = object == null ? "" : object.optString("device_id", "");
        if (isValidDeviceId(fromIntent)) return fromIntent.trim();

        String fromPrefs = runtimePrefs.getString(PREF_SAVED_DEVICE_ID, "");
        if (isValidDeviceId(fromPrefs)) return fromPrefs.trim();

        if (isValidDeviceId(StartAuto.deviceID)) return StartAuto.deviceID.trim();

        try
        {
            String fromSystem = HSQTools.getDeviceSerial(context);
            if (isValidDeviceId(fromSystem)) return fromSystem.trim();
        }
        catch (Exception ignored) {}

        return "";
    }

    private static String getShortDeviceID()
    {
        if (StartAuto.deviceID == null) return "UNKNOWN"; // Đề phòng trường hợp API trả về null

        if (StartAuto.deviceID.length() > 8)
        {
            // Cắt lấy 8 ký tự cuối cùng
            return StartAuto.deviceID.substring(StartAuto.deviceID.length() - 8);
        }

        // Nếu chuỗi ngắn hơn hoặc bằng 8 ký tự thì giữ nguyên
        return StartAuto.deviceID;
    }
    private int AINguL = 0, createAgain = 0, currentState = 0, xs = 0, ysTop = 0, ysBot = 0, swipeDuration = 2000, tryNextAgain = 0, changedSite = 0;
    private boolean isNextCard = false;
    private List<HSQTools.TextBlock> screenBegin = new ArrayList<>();
    private List<HSQTools.TextBlock> phoneScreen = new ArrayList<>();
    private static final int STATE_START = 0, STATE_GET_ANSWER = 1, STATE_ANSWER_OK = 2, STATE_ROLLBACK1 = 3;
    public static boolean isStop = false;
    public static boolean isToolRunning = false;
    public static boolean isUpdating = false;
    private static volatile boolean isRestartingFromVolume = false;

    @SuppressLint("SdCardPath")

    @Override
    public void onStarted(JSONObject object)
    {
        hide();
        HSQTools.setIsRooted(false);
        HSQTools.setIsAdminApp(false);

        Context context = HSQConfig.getContext();
        SharedPreferences runtimePrefs = context.getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE);

        if (object != null && object.has("api_key"))
        {
            apiRun = object.optString("api_key", "").trim();
        }

        if (apiRun == null || apiRun.trim().isEmpty())
        {
            apiRun = context.getSharedPreferences(API_PREFS, Context.MODE_PRIVATE)
                    .getString(PREF_SAVED_API_KEY, "");
            if (apiRun != null) apiRun = apiRun.trim();
        }

        String resolvedDeviceId = resolveDeviceId(object, context, runtimePrefs);
        if (!isValidDeviceId(resolvedDeviceId))
        {
            updateNotificationTitle("SHTools");
            updateNotificationContent("Thieu Device ID - mo app nhap ID");
            StartAuto.isToolRunning = false;
            stopSelf();
            return;
        }

        deviceID = resolvedDeviceId.trim();
        shortDeviceID = getShortDeviceID();
        runtimePrefs.edit().putString(PREF_SAVED_DEVICE_ID, StartAuto.deviceID).apply();

        if (apiRun == null || apiRun.trim().isEmpty())
        {
            updateNotificationTitle(shortDeviceID);
            updateNotificationContent("Thieu API Key - mo app chon API");
            StartAuto.isToolRunning = false;
            stopSelf();
            return;
        }

        context.getSharedPreferences(API_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_SAVED_API_KEY, apiRun)
                .apply();

        if (!isMediaProjectionReady())
        {
            requestScreencastPermissionAndStop(context, runtimePrefs);
            return;
        }

        StartAuto.isToolRunning = true;
        while(true)
        {
            if (deviceID.contains("UNKNOWN"))
            {
                deviceID = HSQTools.getDeviceSerial(HSQConfig.getContext());
                shortDeviceID = getShortDeviceID();
                updateNotificationContent("lỗi deviceID");
                delay(5000);
            }
            else {
                break;
            }
        }
        // --- THÊM ĐOẠN NÀY LƯU DEVICE ID CHO LẦN SAU ---
        runtimePrefs.edit().putString(PREF_SAVED_DEVICE_ID, StartAuto.deviceID).apply();
        loadSurveyBriefMemory();
        // Mở Socket và kích hoạt lại luồng màn hình (lúc này Dịch vụ ngầm đã chạy chính thức)
        RemoteStreamManager.getInstance(this, StartAuto.deviceID).retryStream();

        try
        {
            HSQFileHelper.createFolder(deviceID, imagePath);
            //Truyền service ASBL của sếp cho thư viện
            com.quayquay.hsq.tools.HSQConfig.setASBLService(ASBLBridgeService.asblService);
            //CẮM DÂY CLICK: Chuyền tọa độ từ Thư viện sang cho hàm click của sếp tự múa!
            com.quayquay.hsq.tools.HSQConfig.setASBLBridge(ASBLBridgeService::do_click);
            String directionPath = "/sdcard/Servey/direction.json";
            pathInfoProfileSaved = "/sdcard/Servey/sv_" + deviceID + ".json";
            RegistrationInfo InfoProfile = new RegistrationInfo();
            String profileData = "";
            Instant startTime = Instant.now();

            HSQDevice.setScreenBrightness(HSQConfig.getContext(), 0);
            startTool:
            while (true)
            {
                widthOfScreen = ASBLBridgeService.widthOfScreen;
                if (widthOfScreen == 0) { // Nếu bằng 0 tức là ASBL chưa nạp xong, chờ tí
                    delay(500);
                    continue;
                }
                xCenter = ASBLBridgeService.xCenter;
                heightOfScreen = ASBLBridgeService.heightOfScreen;
                yTop = ASBLBridgeService.yTop;
                yBot = ASBLBridgeService.yBot;
                yCenter = ASBLBridgeService.yCenter;

                updateTitle(shortDeviceID);
                updateNotificationTitle(shortDeviceID);
                updateNotificationContent("Ready...");
                delay(2000);
                loadControl();

                String baseRule = HSQFileHelper.readTextFile("/sdcard/Servey/PromtGem.txt");
                if (profileData.length() < 5)
                {
                    while (true)
                    {
                        if(!updateProfile()) {
                            continue startTool;
                        }

                        try
                        {
                            // 1. Đọc nội dung file JSON
                            File jsonFile = new File(pathInfoProfileSaved);
                            if (jsonFile.exists())
                            {
                                profileData = new String(Files.readAllBytes(Paths.get(pathInfoProfileSaved)));
                                // 2. Gson tương đương MissingMemberHandling.Ignore của Newtonsoft
                                Gson gson = new GsonBuilder().create();
                                InfoProfile = gson.fromJson(profileData, RegistrationInfo.class);

                                break;
                            }
                            else
                            {
                                delay(30000);
                                updateNotificationContent("Thiếu file Info");
                            }
                        }
                        catch (Exception ignore)
                        {
                            delay(30000);
                            updateNotificationContent("Lỗi đọc InfoProfile");
                        }
                    }

                    customAgentRule = baseRule + "\n\nĐÂY LÀ THÔNG TIN CÁ NHÂN CỦA BẠN (HÃY BÁM SÁT VÀO ĐÂY ĐỂ TRẢ LỜI KHẢO SÁT):\n" + profileData;
                }
                if (AIHelper == null)
                {
                    AIHelper = createAIHelper();
                }

                updateNotificationContent("Start...");

                ASBLBridgeService.findMultiTextDesWindow(3, true, true, true, false, "done");

                List<HSQTools.TextBlock> lastScreen;
                List<HSQTools.TextBlock> AllPointsOK = new ArrayList<>();
                Map<String, Integer> matrixColumnCache = new HashMap<>();

                delay(1000);
                beginApp:
                while (true)
                {
                    int LastInterFace = 0, screenSwipe = 0, tempSwipeCount = 0, scanFull, checkloi = 0,
                            timeCheckServey = 0;
                    xs = xCenter;
                    ysTop = yTop;
                    ysBot = yBot;
                    swipeDuration = 2000;
                    String tempTextAnswer = "";
                    boolean daClick, screenDif;
                    String PACK_BITURO = "com.bituro.android.bituro";
                    boolean isAppOpen = HSQTools.isAppOpening(PACK_BITURO);
                    if (!isAppOpen)
                    {
                        clearrecents();
                        createNewChatGemByApi(customAgentRule, true);
                        updateNotificationContent("Open BTR");
                        delay(1000);
                        HSQDevice.openApp(PACK_BITURO);
                        delay(20000);

                        updateNotificationContent("Vào BL");
                        if (HSQTools.getImageExistss(2, false, R.drawable.btr_serveysbl, R.drawable.btr_serveysbl_1) == 0)
                        {
                            clearrecents();
                            delay(2000);
                            continue;
                        }
                        //region --- lấy điểm gửi điểm lên server (BẢN CHỐNG ẢO GIÁC OCR + CHỐNG ĐỌC QUÁ SỚM) ---
                        int accountPoint = findBestBituroAccountPoint();
                        if (accountPoint > 0)
                        {
                            updatePoint(accountPoint + " pts");
                        }
                        else
                        {
                            writeSerLogs("Không tìm được account point hợp lệ ở đầu Bituro");
                        }
//endregion
                        HSQTools.getImageExistss(2, true, R.drawable.btr_serveysbl, R.drawable.btr_serveysbl_1);

                        delay(8000);

                        //region --- Vòng lặp checkSer ---
                        while (true)
                        {
                            int checkServey = HSQTools.getImageExistss(20, false,
                                    R.drawable.btr_accept_all, R.drawable.btr_accept, R.drawable.btr_serveysbl_click, R.drawable.btr_minutes, R.drawable.btr_refreshservey);
                            if (checkServey == 0)
                            {
                                clearrecents();
                                HSQDevice.openApp(PACK_BITURO);
                                delay(20000);

                                if (HSQTools.getImageExistss(2, true, R.drawable.btr_serveysbl, R.drawable.btr_serveysbl_1) == 0)
                                {
                                    clearrecents();
                                    continue beginApp; // goto begin;
                                }
                                delay(10000);
                                continue; // goto checkSer;
                            }
                            else if (checkServey == 1)
                            {
                                HSQTools.getImageExistss(2, true, R.drawable.btr_accept_all);
                                delay(5000);
                                continue;
                            }
                            else if (checkServey == 2)
                            {
                                HSQTools.getImageExistss(2, true, R.drawable.btr_accept);
                                delay(5000);
                                continue;
                            }

                            delay(3000);
                            updateNotificationContent("check servey");

                            while (true)
                            {
                                int checkFind = HSQTools.getImageExistss(5, false, R.drawable.btr_minutes, R.drawable.btr_refreshservey);
                                if(checkFind == 0)
                                {
                                    if (timeCheckServey > 5)
                                    {
                                        globalHome();
                                        delay(2000);
                                        continue startTool;
                                    }
                                    updateNotificationContent("GD Lỗi");
                                    delay(15000);
                                    timeCheckServey++;
                                }
                                else if(checkFind == 2)
                                {
                                    if (timeCheckServey > 5)
                                    {
                                        globalHome();
                                        delay(2000);
                                        continue startTool;
                                    }
                                    updateNotificationContent("không có servey, check lại sau 1 phút");
                                    delay(60000);
                                    HSQTools.getImageExistss(2, true, R.drawable.btr_refreshservey);
                                    delay(5000);
                                    timeCheckServey++;
                                }
                                else
                                {
                                    break;
                                }
                            }

                            //region --- CHECK ĐIỂM KHẢO SÁT (BẢN TỐI ƯU + ĐẦY ĐỦ LOG) ---
                            if (new File(directionPath).exists())
                            {
                                delay(1000);
                            }
                            else
                            {
                                AllPointsOK.clear();
                                List<TextBlock> checkPoints = new ArrayList<>();
                                boolean hasPoints = false;
                                boolean hasRefresh = false;
                                int slsw = 0;

                                updateNotificationContent("Đang săn tìm Points...");

                                for (int w = 0; w < 15; w++)
                                {
                                    AccessibilityNodeInfo root = ASBLBridgeService.asblService.getRootInActiveWindow();
                                    if (root != null)
                                    {
                                        root.refresh();
                                        checkPoints = HSQTools.readAllTextOnScreenByASBL(root);
                                        root.recycle();
                                    }

                                    if (checkPoints == null || checkPoints.size() < 5)
                                    {
                                        updateNotificationContent("ASBL mù (" + w + "), dùng XML Dump...");
                                        String xmlData = HSQTools.getFlexibleXML();
                                        checkPoints = convertXmlToTextBlocks(xmlData);
                                    }

                                    if (checkPoints != null && !checkPoints.isEmpty())
                                    {
                                        hasPoints = checkPoints.stream().anyMatch(x ->
                                                x.text.toLowerCase().contains("points") || x.text.toLowerCase().contains("pts"));
                                        hasRefresh = checkPoints.stream().anyMatch(x -> x.text.toLowerCase().contains("refresh"));

                                        if (hasPoints) break;
                                    }

                                    if (hasRefresh && !hasPoints)
                                    {
                                        TextBlock btnRefresh = checkPoints.stream()
                                                .filter(x -> x.text.toLowerCase().contains("refresh")).findFirst().orElse(null);
                                        if (btnRefresh != null)
                                            click(btnRefresh.x, btnRefresh.y, false);
                                        else click(xCenter, yCenter, false);
                                        delay(5000);
                                        continue startTool;
                                    }

                                    if (w % 3 == 0) swipe(xCenter, yCenter, xCenter, yTop, 300);
                                    delay(1500);
                                }

                                if (!hasPoints)
                                {
                                    updateNotificationContent("Lỗi load thẻ, thử lại...");
                                    delay(3000);
                                    continue beginApp;
                                }

                                checkPoints = checkPoints.stream()
                                        .filter(x -> x.y > 410 && x.y < 2700)
                                        .collect(Collectors.toList());

                                Set<String> seenCards = new HashSet<>();
                                List<TextBlock> orphanMins = new ArrayList<>();
                                Pattern pointPattern = Pattern.compile("([\\d,]+)\\s*(points|pts)", Pattern.CASE_INSENSITIVE);
                                Pattern minutePattern = Pattern.compile("(\\d+\\+?)\\s*(minutes|mins|min)", Pattern.CASE_INSENSITIVE);
                                Pattern starPattern = Pattern.compile("([\\d.]+)\\s*/\\s*5", Pattern.CASE_INSENSITIVE);

                                while (true)
                                {
                                    List<TextBlock> pts = new ArrayList<>();
                                    List<TextBlock> mins = new ArrayList<>();
                                    List<TextBlock> stars = new ArrayList<>();

                                    for (TextBlock b : checkPoints)
                                    {
                                        if (pointPattern.matcher(b.text).find()) pts.add(b);
                                        else if (minutePattern.matcher(b.text).find()) mins.add(b);
                                        else if (starPattern.matcher(b.text).find()) stars.add(b);
                                    }

                                    mins.addAll(orphanMins);
                                    orphanMins.clear();

                                    for (int col = 0; col < 2; col++)
                                    {
                                        int minX = (col == 0) ? 0 : xCenter;
                                        int maxX = (col == 0) ? xCenter : yBot;

                                        List<TextBlock> colPts = pts.stream().filter(b -> b.x >= minX && b.x < maxX)
                                                .sorted(Comparator.comparingInt(b -> b.y)).collect(Collectors.toList());
                                        List<TextBlock> colMins = mins.stream().filter(b -> b.x >= minX && b.x < maxX).collect(Collectors.toList());
                                        List<TextBlock> colStars = stars.stream().filter(b -> b.x >= minX && b.x < maxX).collect(Collectors.toList());

                                        for (TextBlock p : colPts)
                                        {
                                            TextBlock matchedMin = colMins.stream()
                                                    .filter(m -> m.y < p.y + 50 && Math.abs(p.y - m.y) < 800)
                                                    .max(Comparator.comparingInt(m -> m.y)).orElse(null);

                                            TextBlock matchedStar = colStars.stream()
                                                    .filter(s -> s.y > p.y - 50 && Math.abs(s.y - p.y) < 500)
                                                    .min(Comparator.comparingInt(s -> s.y)).orElse(null);

                                            String signature = p.text + "|" + (matchedMin != null ? matchedMin.text : "NO_MIN") + "|" + (matchedStar != null ? matchedStar.text : "NO_STAR");

                                            if (!seenCards.contains(signature))
                                            {
                                                seenCards.add(signature);
                                                AllPointsOK.add(p);
                                                if (matchedMin != null)
                                                {
                                                    AllPointsOK.add(matchedMin);
                                                    colMins.remove(matchedMin);
                                                }
                                                if (matchedStar != null)
                                                {
                                                    AllPointsOK.add(matchedStar);
                                                    colStars.remove(matchedStar);
                                                }
                                            }
                                        }
                                        for (TextBlock m : colMins)
                                        {
                                            if (m.y > 2000) orphanMins.add(m);
                                        }
                                    }

                                    lastScreen = new ArrayList<>(checkPoints);
                                    swipe(xCenter, yBot, xCenter, yTop, 2000);
                                    delay(2000);
                                    slsw++;

                                    AccessibilityNodeInfo root = ASBLBridgeService.asblService.getRootInActiveWindow();
                                    checkPoints = HSQTools.readAllTextOnScreenByASBL(root);
                                    if (checkPoints == null || checkPoints.size() < 3)
                                    {
                                        checkPoints = convertXmlToTextBlocks(HSQTools.getFlexibleXML());
                                    }
                                    if (root != null) root.recycle();

                                    checkPoints = checkPoints.stream().filter(x -> x.y > 410 && x.y < 2700).collect(Collectors.toList());

                                    if (HSQTools.areAlmostSame(checkPoints, lastScreen, 20))
                                    {
                                        for (int k = 0; k < slsw; k++)
                                        {
                                            swipe(xCenter, yTop, xCenter, yBot, 2000);
                                            delay(2000);
                                        }
                                        break;
                                    }
                                }

                                // --- BẮT ĐẦU PHẦN CLICK + LOG ---
                                if (!AllPointsOK.isEmpty())
                                {
                                    TextBlock minPointBlock = null;
                                    TextBlock minMinuteBlock = null;
                                    TextBlock minStarBlock = null;

                                    java.util.List<TextBlock> pointBlocks = new java.util.ArrayList<>();
                                    for (TextBlock b : AllPointsOK) {
                                        if (b.text.toLowerCase().contains("points") || b.text.toLowerCase().contains("pts")) {
                                            pointBlocks.add(b);
                                        }
                                    }

                                    pointBlocks.sort((b1, b2) -> {
                                        int p1 = 0, p2 = 0;
                                        try { p1 = Integer.parseInt(b1.text.replaceAll("[^\\d]", "")); } catch (Exception ignored) {}
                                        try { p2 = Integer.parseInt(b2.text.replaceAll("[^\\d]", "")); } catch (Exception ignored) {}
                                        
                                        int r1 = (p1 >= 90 && p1 < 200) ? 1 : ((p1 >= 0 && p1 < 90) ? 2 : 3);
                                        int r2 = (p2 >= 90 && p2 < 200) ? 1 : ((p2 >= 0 && p2 < 90) ? 2 : 3);
                                        
                                        if (r1 != r2) return Integer.compare(r1, r2);
                                        return Integer.compare(p1, p2);
                                    });

                                    if (!pointBlocks.isEmpty()) {
                                        minPointBlock = pointBlocks.get(0);
                                        int j = AllPointsOK.indexOf(minPointBlock);
                                        
                                        for (int offset = 1; offset <= 2; offset++)
                                        {
                                            if (j + offset < AllPointsOK.size())
                                            {
                                                String nextText = AllPointsOK.get(j + offset).text.toLowerCase();
                                                if (nextText.contains("points") || nextText.contains("pts"))
                                                    break;
                                                if (nextText.contains("min"))
                                                    minMinuteBlock = AllPointsOK.get(j + offset);
                                                else if (nextText.contains("/5"))
                                                    minStarBlock = AllPointsOK.get(j + offset);
                                            }
                                        }
                                    }

                                    if (minPointBlock != null)
                                    {
                                        String pointInfo = minPointBlock.text;
                                        String minuteInfo = minMinuteBlock != null ? minMinuteBlock.text : "NO_MIN";
                                        String starInfo = minStarBlock != null ? minStarBlock.text : "NO_STAR";

                                        // TRẢ LẠI LOG ĐÂY SẾP
                                        writeSerLogs("Click khảo sát: " + pointInfo + " - " + minuteInfo + " - " + starInfo);

                                        String targetPointNum = pointInfo.replaceAll("[^\\d]", "");
                                        boolean answerOK = false;

                                        for (int sc = 0; sc < 15; sc++)
                                        {
                                            checkPoints = HSQTools.readAllTextOnScreenByASBL(ASBLBridgeService.asblService.getRootInActiveWindow());
                                            if (checkPoints.isEmpty())
                                                checkPoints = convertXmlToTextBlocks(HSQTools.getFlexibleXML());

                                            TextBlock targetToClick = checkPoints.stream()
                                                    .filter(x -> x.text.replaceAll("[^\\d]", "").equals(targetPointNum))
                                                    .filter(x -> x.y > 410).findFirst().orElse(null);

                                            if (targetToClick != null)
                                            {
                                                resetSurveyBriefMemory("new_survey_card");
                                                click(targetToClick.x, targetToClick.y, false);
                                                writeSerLogs("Click thành công!"); // LOG THÀNH CÔNG
                                                answerOK = true;
                                                break;
                                            }
                                            swipe(xCenter, yBot, xCenter, yTop, 2000);
                                            delay(2000);
                                        }
                                        if (!answerOK)
                                        {
                                            writeSerLogs("Không tìm thấy tọa độ thẻ trên màn hình để click (Force click tọa độ cố định).");
                                            resetSurveyBriefMemory("new_survey_force_click");
                                            click(668, 1079, false);
                                        }
                                    }
                                    else
                                    {
                                        writeSerLogs("Không bóc tách được Point hợp lệ từ AllPointsOK");
                                    }
                                }
                                else
                                {
                                    writeSerLogs("Danh sách AllPointsOK trống (Không quét được thẻ nào)!");
                                }
                                delay(20000);
                            }
                            //endregion

                            delay(20000);
                            break; // Thoát vòng checkSer khi hoàn tất chọn điểm
                        }
                        //endregion
                    }
                    else
                    {
                        createNewChatGemByApi(customAgentRule, false);
                    }

                    lamProfileLoop:
                    while (true)
                    {
                        //region --- Làm Profile KS ---
                        int checkPRF = HSQTools.getImageExistss(
                                10, false,
                                R.drawable.btr_complete_profile, R.drawable.btr_wefound, R.drawable.btr_profile_match, R.drawable.btr_accept, R.drawable.btr_accept_all
                        );
                        if (checkPRF == 4 || checkPRF == 5)
                        {
                            HSQTools.getImageExistss(2, true, R.drawable.btr_accept, R.drawable.btr_accept_all);
                            delay(5000);
                            continue;
                        }
                        else if (checkPRF == 1)
                        {
                            int checkSetup = HSQTools.getImageExistss(
                                    2, false,
                                    R.drawable.btr_english, R.drawable.btr_gioitinh, R.drawable.btr_zipcode, R.drawable.btr_thunhaptrungbinhgiadinhhangnam, R.drawable.btr_ttvl,
                                    R.drawable.btr_treduoi18, R.drawable.btr_xacnhanmail, R.drawable.btr_start_servey, R.drawable.btr_accept, R.drawable.btr_accept_all
                            );

                            if (checkSetup == 1 && LastInterFace != 1)
                            {
                                updateNotificationContent("chọn language");
                                click(200, 1295, false);
                                delay(2000);
                                click(200, 1535, false);
                                HSQTools.delay(2000);
                                click(640, 2819, false); // continue
                            }// ngon ngu
                            else if (checkSetup == 2 && LastInterFace != 2)
                            {
                                updateNotificationContent("giới tính");
                                if (InfoProfile.getGender() == null || InfoProfile.getGender() == RegistrationInfo.Gender.NONE)
                                {
                                    textAnswer = getAnswerFromGemByApi(1, true, true, "");
                                    updateNotificationContent("giới tính: " + textAnswer);
                                    if (HSQTools.normalizeText(textAnswer).contains("nam"))
                                    {
                                        InfoProfile.setGender(RegistrationInfo.Gender.MALE);
                                    }
                                    else
                                    {
                                        InfoProfile.setGender(RegistrationInfo.Gender.FEMALE);
                                    }
                                    saveServeyData(InfoProfile, pathInfoProfileSaved);
                                }
                                if (InfoProfile.getGender() == RegistrationInfo.Gender.FEMALE)
                                {
                                    HSQTools.getImageExistss(2, true, R.drawable.btr_gioitinh_nu);
                                }
                                else
                                {
                                    HSQTools.getImageExistss(2, true, R.drawable.btr_gioitinh_nam);
                                }
                                HSQTools.delay(3000);
                                click(xCenter, heightOfScreen * 92 / 100, false); // continue
                            }//giới tính
                            else if (checkSetup == 3 && LastInterFace != 3)
                            {
                                updateNotificationContent("zip code");
                                if (InfoProfile.getZipCode() == 0)
                                {
                                    textAnswer = getAnswerFromGemByApi(1, true, true, "");
                                    updateNotificationContent("zip code: " + textAnswer);
                                    if (textAnswer.length() == 5)
                                    {
                                        InfoProfile.setZipCode(Integer.parseInt(textAnswer + "0"));
                                    }
                                    saveServeyData(InfoProfile, pathInfoProfileSaved);
                                }
                                String inputAn = String.valueOf(InfoProfile.getZipCode());
                                if (inputAn.length() > 5)
                                {
                                    inputAn = inputAn.substring(0, 5);
                                }
                                click(734, 1316, false); // zip code
                                HSQTools.delay(2000);
                                clearAllText();
                                HSQTools.delay(2000);

                                inputText(inputAn, null, true);
                                HSQTools.delay(1000);
                                ASBLBridgeService.globalBack();
                                HSQTools.delay(1000);
                                click(xCenter, heightOfScreen * 92 / 100, false); // continue
                            }//zip code
                            else if (checkSetup == 4 && LastInterFace != 4)
                            {
                                updateNotificationContent("Thu nhập trung bình HGĐ hàng năm");
                                click(720, 1400, false);
                                delay(2000);
                                clearAllText();
                                delay(1000);
                                int numSa = (new Random().nextInt(111) + 90) * 10000000;
                                inputText(String.valueOf(numSa), null, true);
                                HSQTools.delay(2000);
                                globalBack();
                                delay(2000);
                                click(xCenter, heightOfScreen * 92 / 100, false); // continue
                            }// thu nhập gia đình hàng năm input
                            else if (checkSetup == 5 && LastInterFace != 5)
                            {
                                updateNotificationContent("TTVL");
                                while (HSQTools.getImageExistss(2, true, R.drawable.btr_ttvl_toanthoigian) == 0)
                                {
                                    swipe(xCenter, yBot, xCenter, yTop, 2000);
                                    HSQTools.delay(2000);
                                }
                                HSQTools.delay(2000);
                                click(xCenter, heightOfScreen * 92 / 100, false); // continue
                            }// thông tin việc làm
                            else if (checkSetup == 6 && LastInterFace != 6)
                            {
                                updateNotificationContent("trẻ dưới 18");
                                while (HSQTools.getImageExistss(2, true, R.drawable.btr_treduoi18_1be) == 0)
                                {
                                    swipe(xCenter, yBot, xCenter, yTop, 2000);
                                    HSQTools.delay(2000);
                                }
                                HSQTools.delay(3000);
                                click(xCenter, heightOfScreen * 92 / 100, false); // continue
                            }// số lượng trẻ dưới 18
                            else if (checkSetup == 7 && LastInterFace != 7)
                            {
                                updateNotificationContent("nhập lại email");
                                textAnswer = InfoProfile.getEmails() != null ? InfoProfile.getEmails() : "";

                                if (!textAnswer.contains("@"))
                                {
                                    updateNotificationContent("Thiếu emails");
                                    HSQTools.delay(120000);
                                }
                                click(704, 1166, false); // cau tl
                                HSQTools.delay(2000);
                                clearAllText();
                                HSQTools.delay(1000);
                                inputText(textAnswer, null, true);
                                HSQTools.delay(2000);
                                ASBLBridgeService.globalBack();
                                HSQTools.delay(2000);
                                click(xCenter, heightOfScreen * 92 / 100, false); // continue
                            }// nhập lại email
                            else if (checkSetup == 8)
                            {
                                HSQTools.getImageExistss(2, true, R.drawable.btr_start_servey);
                                HSQTools.delay(10000);
                                break; // (profile match)
                            }// start servey
                            else if (checkSetup == 9)
                            {
                                HSQTools.getImageExistss(2, true, R.drawable.btr_accept);
                            }//accept
                            else if (checkSetup == 10)
                            {
                                HSQTools.getImageExistss(2, true, R.drawable.btr_accept_all);
                            }//accept
                            else
                            {
                                delay(1000);
                            }

                            LastInterFace = checkSetup;
                            if (checkSetup != 0 && checkSetup < 12)
                            {
                                HSQTools.delay(5000);
                                continue;
                            }
                        }// làm profile
                        else if (checkPRF == 2)
                        {
                            if (HSQTools.getImageExistss(2, true, R.drawable.btr_minutes) == 0)
                            {
                                clearrecents();
                                HSQDevice.openApp(PACK_BITURO);
                                delay(20000);
                                continue beginApp;
                            }
                            delay(10000);
                            continue;
                        }
                        else if (checkPRF == 3)
                        {
                            HSQTools.getImageExistss(2, true, R.drawable.btr_start_servey);
                            delay(60000);
                        }
                        //endregion

                        //region --- Làm KS ---
                        while (true)
                        {
                            scanFull = 0;
                            textAnswer = "";
                            matrixColumnCache.clear();

                            int checkGDKS = HSQTools.getImageExistss(
                                    2, false,
                                    R.drawable.btr_serveyngao, R.drawable.btr_bamtieptuc, R.drawable.btr_captcha, R.drawable.btr_accept, R.drawable.btr_profile_match,
                                    R.drawable.btr_minutes, R.drawable.btr_refreshservey_1, R.drawable.btr_refreshservey, R.drawable.btr_ngaysinh, R.drawable.btr_gioitinh_tuoi_con,
                                    R.drawable.btr_toisinhra, R.drawable.btr_tinh_sv, R.drawable.btr_serveysbl, R.drawable.btr_serveysbl_1, R.drawable.btr_complete_profile,
                                    R.drawable.btr_accept_all, R.drawable.btr_tach
                            );
                            if (checkGDKS == 1)
                            {
                                resetSurveyBriefMemory("survey_exit");
                                clearrecents();
                                HSQTools.delay(2000);
                                continue beginApp; // Thay cho goto begin;
                            }
                            else if (checkGDKS == 2 && LastInterFace != 2)
                            {
                                delay(1000);
                                click(720, 1305, false);
                                delay(10000);
                                continue;
                            }
                            else if (checkGDKS == 3 && LastInterFace != 3)
                            {
                                HSQTools.sendTelegramAlert(deviceID, "captcha", idTelegram);
                                updateNotificationContent("captcha");
                                delay(5000);
                                List<TextBlock> checkUserAct1 = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                while (true)
                                {
                                    HSQTools.delay(180000);
                                    List<TextBlock> checkUserAct2 = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                    if (!HSQTools.areAlmostSame(checkUserAct1, checkUserAct2, 20))
                                    {
                                        break;
                                    }
                                }
                            }
                            else if (checkGDKS == 4)
                            {
                                HSQTools.getImageExistss(2, true, R.drawable.btr_accept);
                            }
                            else if (checkGDKS == 16)
                            {
                                HSQTools.getImageExistss(2, true, R.drawable.btr_accept_all);
                            }
                            else if (checkGDKS == 6 || checkGDKS == 7 || checkGDKS == 8 || checkGDKS == 13 || checkGDKS == 14 || checkGDKS == 17)
                            {
                                if (HSQTools.getImageExistss(2, false, R.drawable.btr_servey_passed) != 0)
                                {
                                    writeSerLogs("servey passed");
                                    resetSurveyBriefMemory("survey_passed");
                                    updateNotificationContent("làm Servey ngon, đang reboot...");
                                    if (!HSQDevice.reboot())
                                    {
                                        ASBLBridgeService.showPowerDialog();
                                        delay(2000);
                                        findAndClickByTextDes("reboot", true, true, true, false, 10);
                                    }
                                }
                                else
                                {
                                    writeSerLogs("tachcmnr");
                                    resetSurveyBriefMemory("survey_failed");
                                    java.time.Duration duration = java.time.Duration.between(startTime, java.time.Instant.now());
                                    long minutesElapsed = duration.toMinutes(); // Lấy phút
                                    if (minutesElapsed >= 120)
                                    {
                                        updateNotificationContent("đang reboot...");
                                        if (!HSQDevice.reboot())
                                        {
                                            ASBLBridgeService.showPowerDialog();
                                            delay(2000);
                                            findAndClickByTextDes("reboot", true, true, true, false, 10);
                                        }
                                    }
                                    else
                                    {
                                        clearrecents();
                                        HSQTools.delay(2000);
                                    }
                                }
                                continue beginApp; // goto begin;
                            }
                            else if (checkGDKS == 5 && LastInterFace != 5)
                            {
                                HSQTools.getImageExistss(2, true, R.drawable.btr_start_servey);
                                HSQTools.delay(30000);
                            }
                            else if (checkGDKS == 11 && LastInterFace != 11)
                            {
                                int MOB = InfoProfile.getMonthOfBirth();
                                int YOB = InfoProfile.getYearOfBirth();
                                String[] mSubs = {"", "thangmot", "thanghai", "thangba", "thangtu", "thangnam", "thangsau", "thangbay", "thangtam", "thangchin", "thangmuoi", "thangmuoimot", "thangmuoihai"};
                                String MSub = mSubs[MOB];

                                updateNotificationContent("chọn năm sinh: " + YOB);
                                swipe(1028, 2325, 1028, 1470, 800);
                                HSQTools.delay(2000);
                                swipe(1028, 2325, 1028, 1470, 800);
                                HSQTools.delay(2000);
                                swipe(1028, 2325, 1028, 1470, 2000);
                                HSQTools.delay(2000);

                                boolean YearOK = false;
                                while (true)
                                {
                                    List<TextBlock> checkYOB = HSQTools.getOcrTextBlocks().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                    for (TextBlock YOBB : checkYOB)
                                    {
                                        int checkY = 0;
                                        try
                                        {
                                            checkY = Integer.parseInt(YOBB.text.replace("O", "0").replaceAll("[^0-9]", ""));
                                        }
                                        catch (Exception ignored)
                                        {
                                        }
                                        if (checkY == YOB)
                                        {
                                            if (YOBB.y < 1850 || YOBB.y > 1980)
                                            {
                                                swipe(1028, YOBB.y, 1028, 1917, 2000);
                                            }
                                            YearOK = true;
                                            break;
                                        }
                                    }
                                    if (YearOK) break;
                                    swipe(1028, 2230, 1028, 1500, 2000);
                                    HSQTools.delay(2000);
                                }

                                boolean MonthOK = false;
                                while (true)
                                {
                                    List<TextBlock> checkMOB = HSQTools.getOcrTextBlocks().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                    for (TextBlock MOBB : checkMOB)
                                    {
                                        String checkY = HSQTools.normalizeText(MOBB.text);
                                        if (checkY.equals(MSub))
                                        {
                                            if (MOBB.y < 1850 || MOBB.y > 1980)
                                            {
                                                swipe(445, MOBB.y, 445, 1917, 2000);
                                            }
                                            MonthOK = true;
                                            break;
                                        }
                                    }
                                    if (MonthOK) break;
                                    swipe(445, 2230, 445, 1500, 2000);
                                    HSQTools.delay(2000);
                                }
                                HSQTools.delay(2000);
                                click(720, 2675, false);
                                HSQTools.delay(6000);
                            }
                            else if (checkGDKS == 0 || checkGDKS == 9 || checkGDKS == 10 || checkGDKS == 12 || checkGDKS == 15)
                            {
                                HSQTools.zoomOut();
                                delay(2000);
                                if (checkGDKS == 9)
                                {
                                    tempTextAnswer = textAnswer = "begin|GDKS = 9|step1 click_point {1100,1360}; step2 clicktotext {" + InfoProfile.getYearOfBirth() + "}; step3 click_point {720,1360};" +
                                            "step4 clicktotext {" + InfoProfile.getMonthOfBirth() + "}; step 5 click_point {215,1360}; step6 clicktotext {" + InfoProfile.getDayOfBirth() + "}; step7 clickbutton {continue}|end";
                                    currentState = 3;
                                }
                                else
                                {
                                    currentState = STATE_START;
                                }

                                boolean swipeDropdown = false;
                                stateMachine:
                                while (true)
                                {
                                    switch (currentState)
                                    {
                                        //region ---STATE_START (0)---
                                        case STATE_START:
                                            if (checkGDKS == 12)
                                            {
                                                delay(5000);
                                                if (HSQTools.getImageExistss(2, true, R.drawable.btr_tinh_sv_acceptall) != 0)
                                                {
                                                    delay(3000);
                                                }
                                                tempTextAnswer = textAnswer = "begin|" + InfoProfile.getProvince() + "|step1 clicktotext {" + InfoProfile.getProvince() + "}; step2 clickbutton {tieptheo};|end";
                                                xs = xCenter;
                                                ysTop = yTop;
                                                ysBot = yBot;
                                                swipeDuration = 2000;
                                                currentState = STATE_ROLLBACK1; // goto rollBack1;
                                                continue;
                                            }

                                            if (scanFull == 0)
                                            {
                                                tempTextAnswer = textAnswer = "begin|swipemore|1|end";
                                                currentState = STATE_ROLLBACK1; // goto rollBack1;
                                                continue;
                                            }

                                            // Rơi tự do xuống getAnswer
                                            currentState = STATE_GET_ANSWER;
                                            continue;
                                            //endregion

                                            //region ---STATE_GET_ANSWER (1)---
                                        case STATE_GET_ANSWER: // Tương đương nhãn getAnswer:
                                            for (int i = 0; i < 10; i++)
                                            {
                                                swipe(xs, ysTop, xs, ysBot, swipeDuration);
                                                delay(2000);
                                            }
                                            tempTextAnswer = textAnswer = "begin|swipemore|1|end";
                                            currentState = STATE_ROLLBACK1; // goto rollBack1;
                                            continue;
                                            //endregion

                                            //region ---STATE_ANSWER_OK (2)---
                                        case STATE_ANSWER_OK: // Tương đương nhãn answerOK:
                                            if (textAnswer.contains("|loicmnr|") || textAnswer.contains("|tachcmnr|"))
                                            {
                                                if (checkloi == 0)
                                                {
                                                    checkloi++;
                                                    screenBegin = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                    textAnswer = sendChatToGemByApi("check lại lần nữa xem có tạch hay lỗi thật không");
                                                    currentState = STATE_ANSWER_OK; // goto answerOK;
                                                    continue;
                                                }
                                                writeSerLogs("answer: " + textAnswer);


                                                java.time.Duration duration = java.time.Duration.between(startTime, java.time.Instant.now());
                                                long minutesElapsed = duration.toMinutes(); // Lấy phút
                                                if (minutesElapsed >= 120)
                                                {
                                                    updateNotificationContent("đang reboot...");
                                                    if (!HSQDevice.reboot())
                                                    {
                                                        ASBLBridgeService.showPowerDialog();
                                                        delay(2000);
                                                        findAndClickByTextDes("reboot", true, true, true, false, 10);
                                                    }
                                                }
                                                else
                                                {
                                                    clearrecents();
                                                    delay(2000);
                                                }

                                                // Tương đương "goto begin;" -> Thoát vòng lặp hiện tại để quay lại beginApp
                                                continue beginApp;
                                            }
                                            else if (textAnswer.contains("captcha"))
                                            {
                                                HSQTools.sendTelegramAlert(deviceID, "captcha", idTelegram);
                                                delay(2000);
                                                updateNotificationContent("captcha");
                                                delay(5000);
                                                List<HSQTools.TextBlock> checkUserAct1 = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                while (true)
                                                {
                                                    delay(180000);
                                                    List<HSQTools.TextBlock> checkUserAct2 = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                    if (!HSQTools.areAlmostSame(checkUserAct1, checkUserAct2, 20))
                                                    {
                                                        break;
                                                    }
                                                }
                                            }
                                            checkloi = 0;

                                            // region check xem có bị che màn không (Tương đương nhãn checkSame:)
                                            while (true)
                                            {
                                                daClick = false;
                                                screenDif = false;
                                                List<TextBlock> checkScreens = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());

                                                if (!HSQTools.areAlmostSame(screenBegin, checkScreens, 20))
                                                {
                                                    String resultNorms = HSQTools.normalizeText("tôi cần thêm thời gian");

                                                    for (TextBlock answer : checkScreens)
                                                    {
                                                        String answerChoose = HSQTools.normalizeText(answer.text);
                                                        if (answerChoose.equals(resultNorms))
                                                        {
                                                            click(answer.x, answer.y, false);
                                                            daClick = true;
                                                            break;
                                                        }
                                                    }
                                                    if (!daClick)
                                                    {
                                                        for (TextBlock answer : checkScreens)
                                                        {
                                                            String answerChoose = HSQTools.normalizeText(answer.text);
                                                            if (answerChoose.contains(resultNorms))
                                                            {
                                                                click(answer.x, answer.y, false);
                                                                daClick = true;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (!daClick)
                                                    {
                                                        for (TextBlock answer : checkScreens)
                                                        {
                                                            String answerChoose = HSQTools.normalizeText(answer.text);
                                                            if (answerChoose.length() < 4 || resultNorms.length() < 4)
                                                                continue;

                                                            int distance = HSQTools.levenshtein(answerChoose, resultNorms);
                                                            int threshHold = (int) (resultNorms.length() * 0.20);
                                                            if (distance <= threshHold)
                                                            {
                                                                click(answer.x, answer.y, false);
                                                                daClick = true;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (daClick)
                                                    {
                                                        continue; // goto checkSame;
                                                    }
                                                    screenDif = true;
                                                }
                                                delay(2000);
                                                break;
                                            }
                                            // endregion

                                            if (screenDif)
                                            {
                                                tempTextAnswer = textAnswer;
                                                currentState = STATE_ROLLBACK1; // goto rollBack1;
                                                continue;
                                            }

                                            if (tempSwipeCount > 0 && !textAnswer.contains("swipemore"))
                                            {
                                                swipeToTop(tempSwipeCount, swipeDropdown);
                                                screenSwipe = 0;
                                                tempSwipeCount = 0;
                                            }

                                            tempTextAnswer = textAnswer;
                                            currentState = STATE_ROLLBACK1;
                                            continue;
                                            //endregion

                                            //region ---STATE_ROLLBACK1 (3)---
                                        case STATE_ROLLBACK1: // Tương đương nhãn rollBack1:

                                            if (tempTextAnswer.contains("|swipemore|"))
                                            {
                                                updateNotificationContent("chuẩn bị màn hình cho GEM");

                                                if (screenSwipe == 0 && swipeDropdown)
                                                {
                                                    activeDropdownSwipeBounds = null;
                                                    String currentXmlForSwipe = HSQTools.getFlexibleXML();
                                                    android.graphics.Rect dropBounds = null;
                                                    int maxArea = 0;

                                                    try {
                                                        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                        org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXmlForSwipe.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                        org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                        for (int i = 0; i < nodes.getLength(); i++) {
                                                            org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                            String clazz = node.getAttribute("class");
                                                            boolean isScrollable = "true".equals(node.getAttribute("scrollable"));
                                                            
                                                            // Bắt chuẩn xác các thẻ chuyên dùng làm Dropdown/Popup hoặc có thuộc tính cuộn
                                                            if (clazz.contains("ListView") || clazz.contains("ScrollView") || clazz.contains("RecyclerView") || clazz.contains("GridView") || isScrollable) {
                                                                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                // Lọc bớt các khối rác quá nhỏ
                                                                if (r != null && r.width() > 300 && r.height() > 250) {
                                                                    int area = r.width() * r.height();
                                                                    if (area > maxArea) {
                                                                        maxArea = area;
                                                                        dropBounds = r;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } catch (Exception ignored) {}

                                                    if (dropBounds != null) {
                                                        activeDropdownSwipeBounds = new android.graphics.Rect(dropBounds);
                                                        // Đã tóm được Dropdown -> Canh tọa độ vuốt GỌN GÀNG TỪNG MILIMET bên trong hộp
                                                        xs = dropBounds.centerX();
                                                        ysBot = dropBounds.bottom - 100; // Trừ hao mép dưới để né nút
                                                        ysTop = dropBounds.top + 100;    // Trừ hao mép trên để né title
                                                        
                                                        // Guard chống văng ra ngoài màn hình
                                                        if (ysBot > yBot) ysBot = yBot;
                                                        if (ysTop < yTop) ysTop = yTop;
                                                        
                                                        // Đảm bảo khoảng cách vuốt có ý nghĩa
                                                        if (ysBot - ysTop < 150) {
                                                            ysBot = yBot;
                                                            ysTop = yTop;
                                                        }
                                                        
                                                        int distance = Math.abs(ysBot - ysTop);
                                                        swipeDuration = Math.max(500, Math.min(2000, distance));
                                                    } else {
                                                        activeDropdownSwipeBounds = null;
                                                        // Fallback nếu không tóm được cái hộp nào ra hồn
                                                        xs = xCenter;
                                                        ysTop = yTop;
                                                        ysBot = yBot;
                                                        swipeDuration = 2000;
                                                    }
                                                }
                                                else if (screenSwipe == 0)
                                                {
                                                    xs = xCenter;
                                                    ysTop = yTop;
                                                    ysBot = yBot;
                                                    swipeDuration = 2000;
                                                }

                                                delay(1000);
                                                HSQFileHelper.deleteFile(imagePath);
                                                HSQFileHelper.createFolder(imagePath);
                                                delay(1000);
                                                HSQTools.captureAndSaveScreen(imagePath + "/screenCapa1.png");

                                                List<HSQTools.TextBlock> beforeSwipe = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                getTopText(beforeSwipe);

                                                while (true)
                                                {
                                                    if (swipeDropdown)
                                                    {
                                                        scrollDropdownForward();
                                                    }
                                                    else
                                                    {
                                                        smartScroll(ysBot, ysTop, "swipemore");
                                                    }
                                                    delay(3000);

                                                    screenBegin = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());

                                                    boolean almostSameAfterSwipe = HSQTools.areAlmostSame(beforeSwipe, screenBegin, 20);
                                                    if (almostSameAfterSwipe && swipeDropdown && screenSwipe < MAX_LOCAL_BLIND_IMAGES - 1)
                                                    {
                                                        forceDropdownSwipeForward();
                                                        delay(1800);
                                                        screenBegin = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                        almostSameAfterSwipe = HSQTools.areAlmostSame(beforeSwipe, screenBegin, 20);
                                                    }

                                                    if (almostSameAfterSwipe || screenSwipe >= MAX_LOCAL_BLIND_IMAGES - 1)
                                                    {
                                                        break;
                                                    }
                                                    screenSwipe++;
                                                    if (screenSwipe + 1 < 10)
                                                    {
                                                        HSQTools.captureAndSaveScreen(imagePath + "/screenCapa" + (screenSwipe + 1) + ".png");
                                                    }
                                                    else
                                                    {
                                                        HSQTools.captureAndSaveScreen(imagePath + "/screenCapb" + (screenSwipe + 1 - 9) + ".png");
                                                    }
                                                    delay(5000);
                                                    beforeSwipe = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                }

                                                tempSwipeCount = tempSwipeCount + screenSwipe;

                                                updateNotificationContent("Local mù/Câu hỏi phụ, bắn " + (screenSwipe + 1) + " ảnh cho API...");
                                                textAnswer = getAnswerFromGemByApi(screenSwipe + 1, false, false, "");

                                                screenSwipe = 0;
                                                scanFull++;
                                                currentState = STATE_ANSWER_OK; // goto answerOK;
                                                continue;
                                            }
                                            else
                                            {
                                                writeSerLogs("answer: " + tempTextAnswer);
                                                String step = "";
                                                try
                                                {
                                                    String[] splitTextAnswer = tempTextAnswer.split("\\|", -1);
                                                    List<String> validSplits = java.util.Arrays.stream(splitTextAnswer).filter(s -> !s.isEmpty()).collect(Collectors.toList());

                                                    // FIX CHÍ MẠNG: Dùng chuẩn Regex của C# đéo thêm bớt chữ nào!
                                                    String[] splitStep = java.util.Arrays.stream(validSplits.get(2).split(";(?![^{]*\\})"))
                                                            .filter(s -> s != null && !s.trim().isEmpty()).toArray(String[]::new);

                                                    int totalClickToTextSteps = 0;
                                                    for (String rawStep : splitStep)
                                                    {
                                                        if (rawStep != null && rawStep.contains("clicktotext"))
                                                        {
                                                            totalClickToTextSteps++;
                                                        }
                                                    }

                                                    int attemptedClickToTextSteps = 0;
                                                    int successClickToTextSteps = 0;
                                                    List<TextBlock> lastClickToTextFailedScreen;
                                                    
                                                    clickToTextMinY = 0; // Reset Y limit cho mỗi vòng trả lời mới

                                                    for (String s : splitStep)
                                                    {
                                                        step = s;
                                                        phoneScreen.clear();
                                                        if (!step.contains("clicktotext") && !step.contains("clickbutton"))
                                                        {
                                                            phoneScreen.addAll(getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList()));
                                                            phoneScreen.add(new TextBlock("Đã vuốt xuống", 0, 0));
                                                        }
                                                        updateNotificationContent("thực hiện: " + step);
                                                        if (step.contains("clicktotext"))
                                                        {
                                                            Matcher match = Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                            if (match.find())
                                                            {
                                                                attemptedClickToTextSteps++;

                                                                List<TextBlock> checkAnswer = clickToText(match.group(1));
                                                                if (checkAnswer != null)
                                                                {
                                                                    lastClickToTextFailedScreen = checkAnswer.stream()
                                                                            .filter(x -> x.y > 180)
                                                                            .collect(Collectors.toList());

                                                                    // Miss 1 cái thì bỏ qua, chỉ fail khi đã thử hết tất cả clicktotext mà vẫn chưa hit được cái nào
                                                                    if (successClickToTextSteps == 0 && attemptedClickToTextSteps >= totalClickToTextSteps)
                                                                    {
                                                                        handleActionFailure(
                                                                                "clicktotext",
                                                                                step,
                                                                                lastClickToTextFailedScreen,
                                                                                "Lỗi clicktotext: Vuốt nát màn hình đéo thấy bất kỳ text nào trong cụm lệnh [" + tempTextAnswer + "]",
                                                                                splitTextAnswer[1]
                                                                        );
                                                                        tempTextAnswer = textAnswer;
                                                                        continue stateMachine;
                                                                    }

                                                                    continue;
                                                                }

                                                                successClickToTextSteps++;
                                                            }
                                                        }
                                                        else if (step.contains("clickbutton"))
                                                        {
                                                            if (checkGDKS == 9)
                                                            {
                                                                click(720, 2829, false);//continue
                                                                delay(10000);
                                                                continue lamProfileLoop;
                                                            }
                                                            isNextCard = false;
                                                            List<TextBlock> currentVisible = clickButton(step);
                                                            if (currentVisible != null)
                                                            {
                                                                swipe(xs, ysBot, xs, ysTop, 1200);
                                                                delay(2000);
                                                                swipe(xs, ysBot, xs, ysTop, 1200);
                                                                delay(2000);
                                                                swipe(xs, ysBot, xs, ysTop, 1200);
                                                                delay(2000);
                                                                swipe(xs, ysBot, xs, ysTop, 1200);
                                                                delay(2000);
                                                                handleActionFailure("clickbutton", step, currentVisible, "Lỗi clickbutton: Tao lật tung màn hình đéo thấy cái nút Next/Continue nào. Mày xem lại ảnh đi!", splitTextAnswer[1]);
                                                                tempTextAnswer = textAnswer;
                                                                continue stateMachine;
                                                            }

                                                            if(isNextCard) {
                                                                delay(5000);
                                                                currentState = 0;
                                                                continue stateMachine;
                                                            }
                                                        }
                                                        else if (step.contains("multi_input"))
                                                        {
                                                            Matcher match = Pattern.compile("\\{([^~]+)~([^}]+)\\}").matcher(step);
                                                            if (match.find())
                                                            {
                                                                String label = match.group(1).trim();
                                                                String values = match.group(2).trim(); // Chứa "Realme|Samsung|Oppo"
                                                                List<TextBlock> result = clickMultiInput(label, values);
                                                                if (result != null)
                                                                {
                                                                    // Nếu trả về Screen -> Có lỗi xảy ra trong quá trình cuộn/nhập
                                                                    handleActionFailure(
                                                                            "Input_Error", step, result.stream().filter(x -> x.y > 180).collect(Collectors.toList()),
                                                                            "Lỗi Input: Tao đã cuộn nát form đéo thấy ô [" + result + "], HOẶC click vào rồi mà bàn phím đéo lên. Mày check lại xem đúng rule không!",
                                                                            splitTextAnswer[1]
                                                                    );
                                                                    tempTextAnswer = textAnswer;
                                                                    continue stateMachine;
                                                                }
                                                            }
                                                        }
                                                        else if (step.contains("input"))
                                                        {
                                                            Matcher match = Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                            if (match.find() && match.group(1).contains("~"))
                                                            {
                                                                String[] parts = match.group(1).split("~");
                                                                String labelToFind = parts[0].trim();
                                                                String valueToInput = parts[1].trim();

                                                                // --- INTERCEPTOR: NẮN NÃO AI NẾU NÓ NHẬP NGU CẤP HÀNH CHÍNH ---
                                                                String labelLower = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(labelToFind));
                                                                String valueLower = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(valueToInput));
                                                                if ((labelLower.contains("quan") || labelLower.contains("huyen") || labelLower.contains("district"))
                                                                        && (valueLower.contains("hanoi") || valueLower.contains("hochiminh") || valueLower.contains("hcm")))
                                                                {
                                                                    handleActionFailure(
                                                                            "Input_Logic", step, getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList()),
                                                                            "Mày bị ngáo à? Ô Quận/Huyện đéo được nhập tên Tỉnh (Hà Nội/HCM). Phải nhập tên 1 quận cụ thể! Hãy sửa lại giá trị nhập.",
                                                                            splitTextAnswer[1]
                                                                    );
                                                                    tempTextAnswer = textAnswer;
                                                                    continue stateMachine;
                                                                }

                                                                // --- GỌI HÀM BỌC THÉP THỰC THI UI ---
                                                                List<TextBlock> resultScreen = clickInput(labelToFind, valueToInput);

                                                                if (resultScreen != null)
                                                                {
                                                                    // Nếu trả về Screen -> Có lỗi xảy ra trong quá trình cuộn/nhập
                                                                    handleActionFailure(
                                                                            "Input_Error", step, resultScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()),
                                                                            "Lỗi Input: Tao đã cuộn nát form đéo thấy ô [" + labelToFind + "], HOẶC click vào rồi mà bàn phím đéo lên. Mày check lại xem đúng rule không!",
                                                                            splitTextAnswer[1]
                                                                    );
                                                                    tempTextAnswer = textAnswer;
                                                                    continue stateMachine;
                                                                }
                                                            }
                                                        }
                                                        else if (step.contains("matrix_dropdown") || step.contains("matrix_click"))
                                                        {
                                                            checkMatrixSmartLoop:
                                                            while (true)
                                                            {
                                                                Matcher match = Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                                if (match.find() && match.group(1).contains("~"))
                                                                {
                                                                    String[] parts = match.group(1).split("~");
                                                                    String rowLabel = parts[0];
                                                                    String colLabel = parts[1];

                                                                    updateNotificationContent("Smart Matrix: " + rowLabel + " -> " + colLabel);
                                                                    List<TextBlock> tempCompare = new ArrayList<>();
                                                                    int vuotTimKiem = 0;     // 0: Đang cuộn xuống tìm HÀNG, 1: Quay xe cuộn lên
                                                                    int vuotNgangLai = 0;    // Theo dõi cuộn ngang tìm CỘT
                                                                    boolean swipeUp = false; // Check xem đã đẩy HÀNG lên giữa màn chưa

                                                                    while (true)
                                                                    {
                                                                        // 0. LẤY DATA MÀN HÌNH THÔNG MINH
                                                                        List<TextBlock> smartList = getCheckAnswerSmart();
                                                                        List<TextBlock> currentVisible = smartList.stream()
                                                                                .filter(x -> x.y > 180 && x.y < 2900).collect(Collectors.toList());
                                                                        phoneScreen.addAll(currentVisible);
                                                                        phoneScreen.add(new TextBlock("Đã vuốt xuống", 0, 0));

                                                                        // =======================================================
                                                                        // 1. TÌM TỌA ĐỘ HÀNG GỐC (BỌC THÉP CHỐNG CẮN NHẦM "AXIORY" VÀ "AXI")
                                                                        // =======================================================
                                                                        String cleanRowLabel = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rowLabel));

                                                                        // TÌM TỌA ĐỘ HÀNG BẰNG VŨ KHÍ TỐI THƯỢNG ĐÃ NÂNG CẤP (QUÉT 3 TẦNG ASBL -> XML -> OCR, 2 LƯỢT STRICT -> LOOSE)
                                                                        android.graphics.Point rowPt = HSQTools.smartFindTextPoint(rowLabel, heightOfScreen, 180, 2900);
                                                                        int targetY = (rowPt != null) ? rowPt.y : -1;

                                                                        // =======================================================
                                                                        // 🚀 XỬ LÝ VUỐT 2 CHIỀU (KHI KHÔNG TÌM THẤY HÀNG)
                                                                        // =======================================================
                                                                        if (rowPt == null)
                                                                        {
                                                                            if (HSQTools.areAlmostSame(tempCompare, currentVisible, 20))
                                                                            {
                                                                                if (vuotTimKiem == 0)
                                                                                {
                                                                                    vuotTimKiem = 1;
                                                                                    updateNotificationContent("Chạm đáy! Quay xe vuốt ngược lên tìm Hàng...");
                                                                                }
                                                                                else
                                                                                {
                                                                                    handleActionFailure(
                                                                                            "Matrix_Row", step, currentVisible,
                                                                                            "Lỗi Matrix: Đã vuốt nát màn hình từ đỉnh xuống đáy mà không thấy hàng [" + rowLabel + "]!, kiểm tra lại rule",
                                                                                            splitTextAnswer[1]
                                                                                    );
                                                                                    tempTextAnswer = textAnswer;
                                                                                    continue stateMachine;
                                                                                }
                                                                            }
                                                                            tempCompare = new ArrayList<>(currentVisible);

                                                                            if (vuotTimKiem == 0)
                                                                            {
                                                                                swipe(xCenter, yBot, xCenter, yTop, 2000); // Vuốt xuống
                                                                            }
                                                                            else
                                                                            {
                                                                                swipe(xCenter, yTop, xCenter, yBot, 2000); // Vuốt lên
                                                                            }
                                                                            delay(2000);
                                                                            continue;
                                                                        }

                                                                        // =======================================================
                                                                        // 🚀 TÌM THẤY HÀNG -> ÉP VỊ TRÍ & PHÂN TÍCH XML
                                                                        // =======================================================
                                                                        // Nếu Hàng nằm sát đáy quá, đẩy nhẹ lên giữa màn hình để lộ XML bên dưới
                                                                        if (rowPt.y > 2300 && !swipeUp)
                                                                        {
                                                                            updateNotificationContent("Hàng nằm sát đáy, kéo lên giữa màn hình...");
                                                                            swipe(720, rowPt.y, 720, 1200, 2000);
                                                                            delay(2000);
                                                                            swipeUp = true;
                                                                            continue;
                                                                        }

                                                                        int preciseY = rowPt.y;
                                                                        int titleRightX = 0;
                                                                        List<android.graphics.Rect> validRadioBoxes = new ArrayList<>();
                                                                        String currentXml = HSQTools.getFlexibleXML();

                                                                        try
                                                                        {
                                                                            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                            // BƯỚC A: Tìm Y chuẩn bằng XML (Không phụ thuộc OCR Y nữa)
                                                                            // Trước tiên thử bám theo XML title node, vì OCR Y có thể sai rất xa
                                                                            int xmlTitleY = -1;
                                                                            for (int i = 0; i < nodes.getLength(); i++)
                                                                            {
                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                String nodeText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(node.getAttribute("text") + " " + node.getAttribute("content-desc")));

                                                                                if (!nodeText.isEmpty() && (nodeText.equals(cleanRowLabel) || nodeText.contains(cleanRowLabel)))
                                                                                {
                                                                                    android.graphics.Rect titleRect = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                    if (titleRect != null && titleRect.centerY() > 180 && titleRect.centerY() < 2900)
                                                                                    {
                                                                                        xmlTitleY = titleRect.centerY();
                                                                                        break;
                                                                                    }
                                                                                }
                                                                            }

                                                                            // Nếu tìm được Y từ XML thì dùng nó, không thì fallback về OCR Y
                                                                            int anchorY = (xmlTitleY > 0) ? xmlTitleY : rowPt.y;

                                                                            for (int offset = -40; offset <= 160; offset += 40)
                                                                            {
                                                                                int testY = anchorY + offset;
                                                                                for (int i = 0; i < nodes.getLength(); i++)
                                                                                {
                                                                                    org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                                                                                    if (r != null && Math.abs(r.centerY() - testY) <= 40 && r.centerX() > (rowPt.x + 100))
                                                                                    {
                                                                                        if (r.width() > 20 && r.width() < 300 && r.height() > 20)
                                                                                        {
                                                                                            preciseY = testY;
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }
                                                                                if (preciseY != anchorY && preciseY != rowPt.y)
                                                                                    break;
                                                                            }
                                                                            // Nếu vẫn chưa tìm được preciseY thì dùng anchorY
                                                                            if (preciseY == rowPt.y && xmlTitleY > 0)
                                                                                preciseY = xmlTitleY;

                                                                            // BƯỚC B: Tìm ranh giới bên phải của chữ Tiêu đề hàng (ĐÃ TÍCH HỢP BỌC THÉP CHỐNG CẮN NHẦM)
                                                                            for (int i = 0; i < nodes.getLength(); i++)
                                                                            {
                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                String nodeText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(node.getAttribute("text") + " " + node.getAttribute("content-desc")));

                                                                                if (!nodeText.isEmpty())
                                                                                {
                                                                                    boolean isMatch = false;
                                                                                    if (nodeText.equals(cleanRowLabel))
                                                                                    {
                                                                                        isMatch = true;
                                                                                    }
                                                                                    else if (nodeText.contains(cleanRowLabel))
                                                                                    {
                                                                                        int lenDiff = nodeText.length() - cleanRowLabel.length();
                                                                                        if (!(cleanRowLabel.length() <= 4 && lenDiff > 1) && lenDiff <= (cleanRowLabel.length() * 0.5))
                                                                                        {
                                                                                            isMatch = true;
                                                                                        }
                                                                                    }

                                                                                    if (isMatch)
                                                                                    {
                                                                                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                        if (r != null && Math.abs(r.centerY() - preciseY) <= 80)
                                                                                        {
                                                                                            titleRightX = r.right + 40; // Lề an toàn
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }

                                                                            if (titleRightX == 0 && rowPt != null)
                                                                            {
                                                                                titleRightX = rowPt.x + (rowLabel.length() * 25) + 50;
                                                                            }
                                                                            if (titleRightX > (widthOfScreen * 0.7))
                                                                                titleRightX = (int) (widthOfScreen * 0.7);

                                                                            // BƯỚC C: Quét SẠCH SẼ các lỗ Radio THẬT (dựa vào Y chuẩn và X của ranh giới chữ)
                                                                            for (int i = 0; i < nodes.getLength(); i++)
                                                                            {
                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                                                                                if (r != null && Math.abs(r.centerY() - preciseY) <= 80 && r.centerX() > titleRightX)
                                                                                {
                                                                                    if (r.width() > 20 && r.width() < 300 && r.height() > 20)
                                                                                    {
                                                                                        validRadioBoxes.add(r);
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                        catch (Exception ignored)
                                                                        {
                                                                        }

                                                                        // =======================================================
                                                                        // 🚀 BƯỚC D: GỘP NODE TRÙNG (DEDUPLICATE)
                                                                        // =======================================================
                                                                        List<android.graphics.Rect> uniqueBoxes = new ArrayList<>();
                                                                        for (android.graphics.Rect r : validRadioBoxes)
                                                                        {
                                                                            boolean isDuplicate = false;
                                                                            for (android.graphics.Rect u : uniqueBoxes)
                                                                            {
                                                                                if (Math.abs(r.centerX() - u.centerX()) < 30)
                                                                                {
                                                                                    isDuplicate = true;
                                                                                    break;
                                                                                }
                                                                            }
                                                                            if (!isDuplicate)
                                                                            {
                                                                                uniqueBoxes.add(r);
                                                                            }
                                                                        }
                                                                        validRadioBoxes = uniqueBoxes;

                                                                        validRadioBoxes.sort(Comparator.comparingInt(r -> r.centerX()));

                                                                        // =======================================================
                                                                        // 🚀 XÁC ĐỊNH TỌA ĐỘ CỘT SỐ CHÍNH XÁC (COL X)
                                                                        // =======================================================
                                                                        int clickX = -1;
                                                                        boolean isNumericCol = colLabel.matches("-?\\d+");

                                                                        // Mới: Bắt tham số thứ 3 (Tổng số cột) nếu có
                                                                        int totalColsExpected = 1;
                                                                        if (parts.length >= 3)
                                                                        {
                                                                            try
                                                                            {
                                                                                totalColsExpected = Integer.parseInt(parts[2].trim());
                                                                            }
                                                                            catch (Exception ignored)
                                                                            {
                                                                            }
                                                                        }

                                                                        if (isNumericCol)
                                                                        {
                                                                            int targetColIndex = Integer.parseInt(colLabel) - 1;
                                                                            String numCacheKey = "MATRIX_COL_IDX_" + targetColIndex;

                                                                            // 1. NẾU CÓ XML XỊN -> Đâm trúng tim đen lỗ Radio
                                                                            if (!validRadioBoxes.isEmpty() && targetColIndex >= 0 && targetColIndex < validRadioBoxes.size())
                                                                            {
                                                                                clickX = validRadioBoxes.get(targetColIndex).centerX();
                                                                                matrixColumnCache.put(numCacheKey, clickX);
                                                                                updateNotificationContent("XML Xịn: Bắt được Cột " + (targetColIndex + 1) + " tại X=" + clickX);
                                                                            }
                                                                            else
                                                                            {
                                                                                // 2. RÚT CACHE NẾU CÓ
                                                                                if (matrixColumnCache.containsKey(numCacheKey))
                                                                                {
                                                                                    clickX = matrixColumnCache.get(numCacheKey);
                                                                                    updateNotificationContent("Rút Cache: Lấy mốc Cột " + (targetColIndex + 1) + " tại X=" + clickX);
                                                                                }
                                                                                else
                                                                                {
                                                                                    // =======================================================
                                                                                    // 🌟 3. FALLBACK TOÁN HỌC (TÍNH TOÁN DỰA TRÊN TỔNG CỘT TỪ KỊCH BẢN)
                                                                                    // =======================================================
                                                                                    // Nếu kịch bản CHƯA truyền đủ 3 tham số, Tool sẽ tự mò bằng Regex (Logic cũ)
                                                                                    if (parts.length < 3)
                                                                                    {
                                                                                        try
                                                                                        {
                                                                                            Matcher m = Pattern.compile("~([^{}]+)\\}").matcher(tempTextAnswer);
                                                                                            int maxNumericCol = 0;
                                                                                            boolean isAllNumeric = true;
                                                                                            Set<String> uniqueTextCols = new HashSet<>();

                                                                                            while (m.find())
                                                                                            {
                                                                                                String valAfterTilde = m.group(1).trim();
                                                                                                uniqueTextCols.add(valAfterTilde);
                                                                                                if (valAfterTilde.matches("-?\\d+"))
                                                                                                {
                                                                                                    int val = Integer.parseInt(valAfterTilde);
                                                                                                    if (val > maxNumericCol)
                                                                                                        maxNumericCol = val;
                                                                                                }
                                                                                                else
                                                                                                {
                                                                                                    isAllNumeric = false;
                                                                                                }
                                                                                            }

                                                                                            if (isAllNumeric && maxNumericCol > 0)
                                                                                                totalColsExpected = maxNumericCol;
                                                                                            else if (!uniqueTextCols.isEmpty())
                                                                                                totalColsExpected = uniqueTextCols.size();
                                                                                        }
                                                                                        catch (Exception e)
                                                                                        {
                                                                                            totalColsExpected = 3;
                                                                                        }
                                                                                    }

                                                                                    // --- BẮT ĐẦU CHIA TỈ LỆ TOÁN HỌC ---
                                                                                    // 🌟 KHÁC BIỆT VỚI DRAGDROP: Matrix có Cột Tiêu Đề bên trái!
                                                                                    // Tool BẮT BUỘC phải bỏ qua Cột Tiêu đề này trước khi chia khoảng trống.

                                                                                    int startX = titleRightX > 0 ? titleRightX : (rowPt.x + (cleanRowLabel.length() * 25) + 30);

                                                                                    // Khóa mốc lề trái không vượt quá 60% màn hình
                                                                                    if (startX > (widthOfScreen * 0.6))
                                                                                        startX = (int) (widthOfScreen * 0.6);

                                                                                    // Vùng không gian CÒN LẠI (Thuộc về các cột đáp án)
                                                                                    int availableWidth = widthOfScreen - startX;

                                                                                    // Chia đều vùng không gian đó cho Tổng số cột đáp án
                                                                                    int stepX = availableWidth / Math.max(1, totalColsExpected);

                                                                                    // Đâm thẳng vào giữa phần không gian của cột được chọn
                                                                                    clickX = startX + (targetColIndex * stepX) + (stepX / 2);

                                                                                    matrixColumnCache.put(numCacheKey, clickX);
                                                                                    updateNotificationContent("Math Mode: Bảng " + totalColsExpected + " Cột. Chọt Cột " + (targetColIndex + 1) + " tại X=" + clickX);
                                                                                }
                                                                            }
                                                                        }
                                                                        else
                                                                        {
                                                                            // =======================================================
                                                                            // 🚀 TÌM CỘT BẰNG CHỮ (TEXT-BASED COLUMN) - ANTI QUESTION TRAP
                                                                            // =======================================================
                                                                            String textCacheKey = "MATRIX_COL_TXT_" + colLabel;
                                                                            int headerX = -1;

                                                                            // 1. RÚT CACHE NẾU CÓ
                                                                            if (matrixColumnCache.containsKey(textCacheKey))
                                                                            {
                                                                                headerX = matrixColumnCache.get(textCacheKey);
                                                                                updateNotificationContent("Universal Matrix: Rút Cache Cột Text [" + colLabel + "] tại X=" + headerX);
                                                                            }
                                                                            else
                                                                            {
                                                                                // Gemini nhả: {IC Markets~Chua su dung} -> cleanColLabel = chuasudung
                                                                                String cleanColLabel = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(colLabel));
                                                                                final int finalPreciseY = preciseY;

                                                                                // 🌟 BƯỚC A: TÍNH TOÁN LỀ TRÁI AN TOÀN (Bỏ qua Tiêu đề hàng)
                                                                                int safeStartX = titleRightX > 0 ? titleRightX : (rowPt != null ? rowPt.x + 250 : 300);

                                                                                // 🌟 BƯỚC B: LỌC ỨNG VIÊN (Nằm bên phải lề & Nằm trên hàng Radio)
                                                                                List<TextBlock> candidates = currentVisible.stream()
                                                                                        .filter(t -> t.y < (finalPreciseY - 30) && t.x > safeStartX)
                                                                                        .collect(Collectors.toList());

                                                                                if (!candidates.isEmpty())
                                                                                {
                                                                                    // 🌟 BƯỚC C: TÌM ĐÁY CỦA TIÊU ĐỀ (BASELINE Y)
                                                                                    // Lấy tọa độ Y của chữ nằm THẤP NHẤT (Sát với hàng Radio nhất)
                                                                                    int headerBaselineY = candidates.stream().mapToInt(t -> t.y).max().orElse(0);

                                                                                    // 🌟 BƯỚC D: LƯỚI LỌC ĐÁY (Chém chết Câu hỏi)
                                                                                    // Chỉ giữ lại những chữ nằm quanh quẩn cái Đáy này (Sai số 120px để bao trọn Tiêu đề nhiều dòng)
                                                                                    List<TextBlock> actualHeaders = candidates.stream()
                                                                                            .filter(t -> Math.abs(t.y - headerBaselineY) <= 120)
                                                                                            .collect(Collectors.toList());

                                                                                    // 🌟 BƯỚC E: GOM NHÓM THEO TRỤC X (CỘT)
                                                                                    Map<Integer, List<TextBlock>> columnsMap = new HashMap<>();
                                                                                    for (TextBlock t : actualHeaders)
                                                                                    {
                                                                                        boolean added = false;
                                                                                        for (Integer colX : columnsMap.keySet())
                                                                                        {
                                                                                            if (Math.abs(t.x - colX) < 60)
                                                                                            { // Sai số X nới lên 60px
                                                                                                columnsMap.get(colX).add(t);
                                                                                                added = true;
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                        if (!added)
                                                                                        {
                                                                                            List<TextBlock> newList = new ArrayList<>();
                                                                                            newList.add(t);
                                                                                            columnsMap.put(t.x, newList);
                                                                                        }
                                                                                    }

                                                                                    // 🌟 BƯỚC F: SO KHỚP CHUỖI THÔNG MINH
                                                                                    for (Map.Entry<Integer, List<TextBlock>> entry : columnsMap.entrySet())
                                                                                    {
                                                                                        List<TextBlock> cluster = entry.getValue();
                                                                                        cluster.sort(Comparator.comparingInt(t -> t.y)); // Xếp Y từ trên xuống

                                                                                        StringBuilder combinedTextBuilder = new StringBuilder();
                                                                                        for (TextBlock t : cluster)
                                                                                        {
                                                                                            combinedTextBuilder.append(t.text).append(" ");
                                                                                        }
                                                                                        String combinedText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(combinedTextBuilder.toString()));

                                                                                        boolean isMatch = false;

                                                                                        // 1. Khớp tuyệt đối
                                                                                        if (combinedText.equals(cleanColLabel))
                                                                                        {
                                                                                            isMatch = true;
                                                                                        }
                                                                                        // 2. Khớp chứa (Chỉ áp dụng cho từ dài > 3 ký tự, cấm áp dụng cho "co" để tránh cắn nhầm)
                                                                                        else if (cleanColLabel.length() > 3 && combinedText.contains(cleanColLabel))
                                                                                        {
                                                                                            int lenDiff = combinedText.length() - cleanColLabel.length();
                                                                                            if (lenDiff <= (cleanColLabel.length() * 0.5))
                                                                                                isMatch = true;
                                                                                        }
                                                                                        // 3. Sai số Levenshtein (CỨU CÁNH CHO CHỮ "CO")
                                                                                        // Từ 1-4 ký tự cho phép sai 1 lỗi (Co -> c6 vẫn bắt được)
                                                                                        else
                                                                                        {
                                                                                            int allowedErr = Math.max(1, (int) (cleanColLabel.length() * 0.25));
                                                                                            if (HSQTools.levenshtein(combinedText, cleanColLabel) <= allowedErr)
                                                                                            {
                                                                                                isMatch = true;
                                                                                            }
                                                                                        }

                                                                                        if (isMatch)
                                                                                        {
                                                                                            headerX = entry.getKey();
                                                                                            updateNotificationContent("OCR Header: Bắt trúng cột [" + colLabel + "] tại X=" + headerX);
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }

                                                                                if (headerX != -1)
                                                                                {
                                                                                    matrixColumnCache.put(textCacheKey, headerX);
                                                                                }
                                                                            }

                                                                            if (headerX != -1)
                                                                            {
                                                                                clickX = headerX;
                                                                                // =======================================================
                                                                                // 🚀 SMART SNAPPING
                                                                                // =======================================================
                                                                                if (!validRadioBoxes.isEmpty())
                                                                                {
                                                                                    final int finalHeaderX = headerX;
                                                                                    android.graphics.Rect closestBox = validRadioBoxes.stream()
                                                                                            .filter(r -> Math.abs(r.centerX() - finalHeaderX) <= 180) // Nới rộng snap
                                                                                            .min(Comparator.comparingInt(r -> Math.abs(r.centerX() - finalHeaderX)))
                                                                                            .orElse(null);

                                                                                    if (closestBox != null)
                                                                                    {
                                                                                        clickX = closestBox.centerX();
                                                                                        updateNotificationContent("Snap! Hít Text-Column vào tâm lỗ tại X=" + clickX);
                                                                                    }
                                                                                }
                                                                                else
                                                                                {
                                                                                    updateNotificationContent("XML Mù: Bắn thẳng tọa độ OCR X=" + clickX);
                                                                                }
                                                                            }
                                                                        }

                                                                        // =======================================================
                                                                        // 🚀 XỬ LÝ KHÔNG TÌM THẤY CỘT (VUỐT NGANG BẢNG)
                                                                        // =======================================================
                                                                        if (clickX == -1)
                                                                        {
                                                                            if (vuotNgangLai < 3)
                                                                            {
                                                                                updateNotificationContent("Thiếu cột dữ liệu, đang cuộn bảng sang bên phải...");
                                                                                swipe(1320, preciseY, 120, preciseY, 1200);
                                                                                delay(2500);
                                                                                vuotNgangLai++;
                                                                                continue;
                                                                            }

                                                                            swipe(120, preciseY, 1320, preciseY, 1200);
                                                                            delay(2000);
                                                                            handleActionFailure(
                                                                                    "Matrix_Col", step, currentVisible,
                                                                                    "Lỗi Matrix: Đã lật bảng sang phải nhưng không thấy cột [" + colLabel + "]!",
                                                                                    splitTextAnswer[1]
                                                                            );
                                                                            tempTextAnswer = textAnswer;
                                                                            continue stateMachine;
                                                                        }

                                                                        // =======================================================
                                                                        // 🚀 THỰC THI CHỌT CHÍNH XÁC VÀO TIM ĐEN
                                                                        // =======================================================
                                                                        if (preciseY != -1 && clickX != -1)
                                                                        {
                                                                            if (step.contains("matrix_dropdown"))
                                                                            {
                                                                                click(clickX, preciseY, false);
                                                                                delay(2000);
                                                                                swipeDropdown = true;
                                                                                currentState = STATE_ROLLBACK1;
                                                                                tempTextAnswer = textAnswer = "begin|swipemore|1|end";
                                                                                continue stateMachine;
                                                                            }
                                                                            else
                                                                            {
                                                                                click(clickX, preciseY, false);
                                                                                delay(1500);
                                                                                break checkMatrixSmartLoop;
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                break;
                                                            }
                                                        }
                                                        else if (step.contains("dragdrop"))
                                                        {
                                                            dragDropLoop:
                                                            while (true)
                                                            {
                                                                Matcher match = Pattern.compile("\\{([^}]+)\\}").matcher(step);
                                                                if (match.find() && match.group(1).contains("~"))
                                                                {
                                                                    String[] parts = match.group(1).split("~");
                                                                    // Hỗ trợ mượt mà cả kịch bản mới (3 tham số) và cũ (2 tham số)
                                                                    if (parts.length >= 2)
                                                                    {
                                                                        String sourceStr = parts[0].trim();
                                                                        String targetStr = parts[1].trim();

                                                                        // NẾU CÓ THAM SỐ THỨ 3 -> LẤY LÀM TỔNG SỐ CỘT. KHÔNG CÓ THÌ MẶC ĐỊNH LÀ 6.
                                                                        int totalColsExpected = (parts.length == 3) ? Integer.parseInt(parts[2].trim()) : 6;

                                                                        updateNotificationContent("Smart DragDrop: " + sourceStr + " -> Cột " + targetStr + "/" + totalColsExpected);
                                                                        List<TextBlock> tempCompare = new ArrayList<>();
                                                                        int vuotLenLai = 0;

                                                                        boolean isNumericTarget = targetStr.matches("-?\\d+");

                                                                        while (true)
                                                                        {
                                                                            // 1. Lấy Data Màn hình để check kẹt
                                                                            List<TextBlock> currentScreen = getCheckAnswerSmart();
                                                                            phoneScreen.addAll(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
                                                                            phoneScreen.add(new TextBlock("Đã vuốt xuống", 0, 0));
                                                                            List<TextBlock> currentVisible = currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList());

                                                                            // 2. Dùng mắt thần tìm Tâm Thẻ Cần Kéo
                                                                            android.graphics.Point sourcePt = HSQTools.smartFindTextPoint(sourceStr, heightOfScreen);
                                                                            android.graphics.Point targetPt;

                                                                            // 3. 🌟 TOÁN HỌC THUẦN TÚY (PURE MATH) - NHANH, GỌN, CHÍNH XÁC 100%
                                                                            if (isNumericTarget)
                                                                            {
                                                                                int targetColIndex = Integer.parseInt(targetStr) - 1;

                                                                                // Ép ranh giới an toàn lỡ AI ngáo
                                                                                if (targetColIndex < 0)
                                                                                    targetColIndex = 0;
                                                                                if (targetColIndex >= totalColsExpected)
                                                                                    targetColIndex = totalColsExpected - 1;

                                                                                // Cắt màn hình ra làm N phần bằng nhau
                                                                                int stepX = widthOfScreen / Math.max(1, totalColsExpected);

                                                                                // Đâm thẳng Tâm X vào giữa cái cột được chọn
                                                                                int dropX = (targetColIndex * stepX) + (stepX / 2);

                                                                                // Fix cứng Y ném vào 70% màn hình
                                                                                int dropY = (int) (heightOfScreen * 0.7);

                                                                                targetPt = new android.graphics.Point(dropX, dropY);
                                                                            }
                                                                            else
                                                                            {
                                                                                targetPt = HSQTools.smartFindTextPoint(targetStr, heightOfScreen);
                                                                            }

                                                                            // 4. THỰC THI VUỐT
                                                                            if (sourcePt != null && targetPt != null)
                                                                            {
                                                                                updateNotificationContent("Kéo [" + sourceStr + "] -> X=" + targetPt.x + ", Y=" + targetPt.y);
                                                                                swipe(sourcePt.x, sourcePt.y, targetPt.x, targetPt.y, 2000);
                                                                                delay(2500);
                                                                                break dragDropLoop; // Xong là dứt điểm, văng ra ngoài đọc step tiếp theo
                                                                            }

                                                                            // 5. XỬ LÝ LỖI (KHÔNG THẤY THẺ)
                                                                            if (sourcePt == null || HSQTools.areAlmostSame(tempCompare, currentVisible, 20))
                                                                            {
                                                                                if (vuotLenLai < 2)
                                                                                {
                                                                                    tempCompare = currentVisible;
                                                                                    updateNotificationContent("Không thấy thẻ [" + sourceStr + "], đang vuốt tìm...");
                                                                                    swipe(xCenter, yBot, xCenter, yTop, 2000);
                                                                                    delay(2000);
                                                                                    vuotLenLai++;
                                                                                    continue; // Quay lại while trong
                                                                                }

                                                                                handleActionFailure(
                                                                                        "DragDrop", step, currentVisible,
                                                                                        "Lỗi kéo thả: Tao đéo tìm thấy thẻ [" + sourceStr + "] để kéo, kiểm tra lại xem có đúng rule không.",
                                                                                        splitTextAnswer[1]
                                                                                );
                                                                                tempTextAnswer = textAnswer;
                                                                                continue stateMachine;
                                                                            }
                                                                            tempCompare = currentVisible;
                                                                        }
                                                                    }
                                                                }
                                                                break;
                                                            }
                                                        }
                                                        else if (step.contains("dropdown"))
                                                        {
                                                            Matcher match = Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                            if (match.find())
                                                            {
                                                                String contextStr = match.group(1).trim();
                                                                updateNotificationContent("Smart Dropdown: " + contextStr);

                                                                // Gọi hàm siêu cấp
                                                                List<TextBlock> resultScreen = clickDropDown(contextStr);
                                                                // NẾU RESULT KHÁC NULL TỨC LÀ LỖI
                                                                if (resultScreen != null)
                                                                {
                                                                    handleActionFailure(
                                                                            "Dropdown", step, resultScreen,
                                                                            "Tao đã lật tung màn hình nhưng đéo tìm thấy cái Dropdown của [" + contextStr + "] nào để mở. Mày check lại xem nó có trên màn hình không!",
                                                                            splitTextAnswer[1]
                                                                    );
                                                                    tempTextAnswer = textAnswer;
                                                                    continue stateMachine;
                                                                }
                                                                tempSwipeCount = 0;
                                                                swipeDropdown = true;
                                                                tempTextAnswer = textAnswer = "begin|swipemore|1|end";
                                                                currentState = STATE_ROLLBACK1;
                                                                continue stateMachine;
                                                            }
                                                        }
                                                        else if (step.contains("click_index"))
                                                        {
                                                            Matcher match = Pattern.compile("\\{(\\d+)\\}").matcher(step);
                                                            if (match.find())
                                                            {
                                                                int targetIndex = Integer.parseInt(match.group(1));
                                                                int currentGlobalCount = 0;
                                                                int vuotLenLai = 0;
                                                                int ignoreYLimit = 300;
                                                                List<TextBlock> tempCompare = new ArrayList<>();

                                                                while (true)
                                                                {
                                                                    List<android.graphics.Rect> clickableRegions = new ArrayList<>();
                                                                    final int currentIgnoreYLimit = ignoreYLimit;

                                                                    // 1. TẦNG 1 & 2: DÙNG ASBL VÀ XML TÌM CHECKBOX/RADIO CHUẨN
                                                                    AccessibilityNodeInfo root = ASBLBridgeService.asblService.getRootInActiveWindow();
                                                                    if (root != null)
                                                                    {
                                                                        clickableRegions = findAllCheckboxesByASBL(root, currentIgnoreYLimit);
                                                                        root.recycle();
                                                                    }

                                                                    if (clickableRegions.isEmpty())
                                                                    {
                                                                        String xml = HSQTools.getFlexibleXML();
                                                                        clickableRegions = findCheckboxesInXml(xml, currentIgnoreYLimit);
                                                                    }

                                                                    boolean isImageCardFallback = false;
                                                                    // 2.5. TẦNG XML ẢNH CỠ LỚN (CARD)
                                                                    if (clickableRegions.isEmpty())
                                                                    {
                                                                        String xml = HSQTools.getFlexibleXML();
                                                                        clickableRegions = findImageCardsInXml(xml, currentIgnoreYLimit);
                                                                        if (!clickableRegions.isEmpty()) isImageCardFallback = true;
                                                                    }

                                                                    // 2.7 TẦNG 3: THỦ MÔN OCR (FALLBACK KHI WEBVIEW GIẤU CLASS VÀ KHÔNG CÓ ẢNH)
                                                                    if (clickableRegions.isEmpty())
                                                                    {
                                                                        updateNotificationContent("Dùng OCR dóng hàng tìm ô thứ " + targetIndex);
                                                                        List<TextBlock> ocrNodes = HSQTools.getOcrTextBlocks();
                                                                        for (TextBlock node : ocrNodes)
                                                                        {
                                                                            if (node.y > currentIgnoreYLimit && node.y < 2900 && node.x < 1000)
                                                                            {
                                                                                clickableRegions.add(new android.graphics.Rect(node.x - 100, node.y - 30, node.x - 20, node.y + 30));
                                                                            }
                                                                        }
                                                                    }

                                                                    // Sắp xếp các vùng tìm thấy: Từ trên xuống dưới, từ trái qua phải (đọc như sách)
                                                                    clickableRegions.sort((a, b) ->
                                                                    {
                                                                        if (Math.abs(a.top - b.top) < 100)
                                                                            return Integer.compare(a.left, b.left);
                                                                        return Integer.compare(a.top, b.top);
                                                                    });

                                                                    // 3. THỰC THI CLICK HOẶC VUỐT TÌM TIẾP
                                                                    if (!clickableRegions.isEmpty())
                                                                    {
                                                                        if (targetIndex <= currentGlobalCount + clickableRegions.size())
                                                                        {
                                                                            int localIndex = targetIndex - currentGlobalCount - 1;
                                                                            if (localIndex >= 0 && localIndex < clickableRegions.size())
                                                                            {
                                                                                android.graphics.Rect targetRect = clickableRegions.get(localIndex);
                                                                                int clickX = targetRect.centerX();
                                                                                int clickY = targetRect.centerY();
                                                                                
                                                                                if (isImageCardFallback) {
                                                                                    int gapRightStart = targetRect.right;
                                                                                    int gapRightEnd = widthOfScreen;
                                                                                    int gapLeftStart = 0;
                                                                                    int gapLeftEnd = targetRect.left;
                                                                                    
                                                                                    boolean hasRightGap = (gapRightEnd - gapRightStart) > (widthOfScreen * 0.12);
                                                                                    boolean hasLeftGap = (gapLeftEnd - gapLeftStart) > (widthOfScreen * 0.12);
                                                                                    
                                                                                    for (android.graphics.Rect other : clickableRegions) {
                                                                                        if (other == targetRect) continue;
                                                                                        if (Math.abs(other.centerY() - clickY) < 100) {
                                                                                            if (hasRightGap && other.left > clickX && other.left < gapRightEnd) hasRightGap = false;
                                                                                            if (hasLeftGap && other.right < clickX && other.right > gapLeftStart) hasLeftGap = false;
                                                                                        }
                                                                                    }
                                                                                    
                                                                                    if (hasRightGap) {
                                                                                        clickX = gapRightStart + (gapRightEnd - gapRightStart) / 2;
                                                                                        updateNotificationContent("Radio ẩn: Click bên phải ảnh");
                                                                                    } else if (hasLeftGap) {
                                                                                        clickX = gapLeftStart + (gapLeftEnd - gapLeftStart) / 2;
                                                                                        updateNotificationContent("Radio ẩn: Click bên trái ảnh");
                                                                                    }
                                                                                }
                                                                                
                                                                                click(clickX, clickY, false);
                                                                                delay(2000);
                                                                                break; // Thành công
                                                                            }
                                                                        }

                                                                        // Nếu chưa tới index cần tìm -> Vuốt màn hình
                                                                        currentGlobalCount += clickableRegions.size();
                                                                        int lastY = clickableRegions.get(clickableRegions.size() - 1).top;
                                                                        tempCompare = getCheckAnswerSmart(); // Lưu lại để check kẹt
                                                                        phoneScreen.addAll(tempCompare.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
                                                                        phoneScreen.add(new TextBlock("Đã vuốt xuống", 0, 0));

                                                                        updateNotificationContent("Đã đếm " + currentGlobalCount + " ô. Vuốt tiếp...");
                                                                        swipe(720, lastY, 720, 700, 2000);
                                                                        delay(2500);
                                                                        ignoreYLimit = 450;
                                                                        vuotLenLai++;
                                                                    }

                                                                    // 4. KIỂM TRA LỖI & BÁO TELEGRAM VIP
                                                                    List<TextBlock> currentVisible = getCheckAnswerSmart();
                                                                    if (clickableRegions.isEmpty() || HSQTools.areAlmostSame(tempCompare, currentVisible, 20) || vuotLenLai >= 5)
                                                                    {
                                                                        handleActionFailure(
                                                                                "Index", step, getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList()),
                                                                                "Mày bảo chọn ô thứ " + targetIndex + " nhưng tao đếm nát cả màn hình chỉ thấy " + currentGlobalCount + " ô. Đếm lại đi con trai!",
                                                                                splitTextAnswer[1]
                                                                        );
                                                                        tempTextAnswer = textAnswer;
                                                                        continue stateMachine;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        else if (step.contains("accordion"))
                                                        {
                                                            // Gọi hàm bọc thép
                                                            List<TextBlock> resultScreen = clickAccordion(step);

                                                            // NẾU RESULT KHÁC NULL -> BÁO LỖI
                                                            if (resultScreen != null)
                                                            {
                                                                handleActionFailure(
                                                                        "Accordion", step, resultScreen,
                                                                        "Tao đã thao tác theo lệnh [" + step + "] nhưng bị kẹt (Không tìm thấy Header, hoặc không thấy Đáp án). Mày kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé.",
                                                                        splitTextAnswer[1]
                                                                );
                                                                tempTextAnswer = textAnswer;
                                                                continue stateMachine;
                                                            }

                                                        }
                                                        else if (step.contains("click_block"))
                                                        {
                                                            while (true)
                                                            {
                                                                Matcher match = Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                                if (match.find() && match.group(1).contains("~"))
                                                                {
                                                                    String[] parts = match.group(1).split("~");
                                                                    String headerStr = parts[0];
                                                                    String answerStr = parts[1];

                                                                    updateNotificationContent("Smart Block: " + headerStr + " -> " + answerStr);
                                                                    List<TextBlock> tempCompare = new ArrayList<>();
                                                                    int vuotLenLai = 0;
                                                                    int keoHeader = 0; // Đếm số lần kéo Header để chống kẹt đáy

                                                                    boolean daThayHeader = false;

                                                                    while (true)
                                                                    {
                                                                        // 0. LẤY DỮ LIỆU MÀN HÌNH THÔNG MINH
                                                                        List<TextBlock> smartList = getCheckAnswerSmart();
                                                                        phoneScreen.addAll(smartList.stream().filter(x -> x.y > 180 && x.y < 2900).collect(Collectors.toList()));
                                                                        phoneScreen.add(new TextBlock("Đã vuốt xuống", 0, 0));
                                                                        List<TextBlock> currentVisible = smartList.stream()
                                                                                .filter(x -> x.y > 180 && x.y < 2900).collect(Collectors.toList());

                                                                        // =======================================================
                                                                        // 1. TÌM TIÊU ĐỀ CÂU HỎI (HEADER) - LAI GHÉP OCR & XML
                                                                        // =======================================================eee
                                                                        int targetY = -1;
                                                                        String cleanHeader = HSQTools.normalizeText(headerStr).replaceAll("[^a-z0-9]", "");

                                                                        // TÌM BẰNG OCR TRƯỚC
                                                                        // Tầng 1: Khớp tuyệt đối 100%
                                                                        // 🌟 ĐẶC TRỊ BỆNH AI LƯỜI NHƯ Ý SẾP: Bỏ qua phần chữ trong ngoặc
                                                                        // Tầng 2: Node chứa Header (Ví dụ tìm Axi, chống cắn nhầm Axiory)
                                                                        // Tầng 3: Header chứa Node (Khóa tử hình lỗi tìm Axiory cắn Axi)
                                                                        // Chỉ cho phép nếu chữ quét được đủ dài và gần bằng Header
                                                                        // Tầng 4: Sai số Levenshtein toàn bộ
                                                                        // Tầng 5: Cắt tiền tố (Prefix matching) - Dành cho câu hỏi cõng giải thích
                                                                        // 🌟 THUẬT TOÁN SẮP XẾP MỚI:
                                                                        // Ưu tiên 1: Chữ nào khớp chính xác 100% thì đưa lên Top 1.
                                                                        // Ưu tiên 2: Nếu cùng độ chính xác, ưu tiên chữ nằm gần đỉnh màn hình nhất (Y nhỏ nhất).
                                                                        TextBlock foundHeader = currentVisible.stream()
                                                                                .filter(x ->
                                                                                {
                                                                                    String nodeTxt = HSQTools.normalizeText(x.text).replaceAll("[^a-z0-9]", "");
                                                                                    if (nodeTxt.isEmpty())
                                                                                        return false;

                                                                                    // Tầng 1: Khớp tuyệt đối 100%
                                                                                    if (nodeTxt.equals(cleanHeader))
                                                                                        return true;

                                                                                    // 🌟 ĐẶC TRỊ BỆNH AI LƯỜI NHƯ Ý SẾP: Bỏ qua phần chữ trong ngoặc
                                                                                    if (x.text.contains("(") || x.text.contains("["))
                                                                                    {
                                                                                        String beforeBracket = x.text.split("[(\\[]")[0];
                                                                                        String cleanBefore = HSQTools.normalizeText(beforeBracket).replaceAll("[^a-z0-9]", "");
                                                                                        if (!cleanBefore.isEmpty() && cleanBefore.equals(cleanHeader))
                                                                                            return true;
                                                                                    }

                                                                                    // Tầng 2: Node chứa Header (Ví dụ tìm Axi, chống cắn nhầm Axiory)
                                                                                    if (nodeTxt.contains(cleanHeader))
                                                                                    {
                                                                                        int lenDiff = nodeTxt.length() - cleanHeader.length();
                                                                                        if (cleanHeader.length() <= 4 && lenDiff > 1)
                                                                                            return false;
                                                                                        if (lenDiff <= (cleanHeader.length() * 0.5))
                                                                                            return true;
                                                                                    }

                                                                                    // Tầng 3: Header chứa Node (Khóa tử hình lỗi tìm Axiory cắn Axi)
                                                                                    // Chỉ cho phép nếu chữ quét được đủ dài và gần bằng Header
                                                                                    if (cleanHeader.contains(nodeTxt))
                                                                                    {
                                                                                        int lenDiff = cleanHeader.length() - nodeTxt.length();
                                                                                        if (nodeTxt.length() > 3 && lenDiff <= 2)
                                                                                            return true;
                                                                                    }

                                                                                    // Tầng 4: Sai số Levenshtein toàn bộ
                                                                                    if (HSQTools.levenshtein(nodeTxt, cleanHeader) <= Math.max(1, (int) (cleanHeader.length() * 0.2)))
                                                                                        return true;

                                                                                    // Tầng 5: Cắt tiền tố (Prefix matching) - Dành cho câu hỏi cõng giải thích
                                                                                    if (nodeTxt.length() >= cleanHeader.length())
                                                                                    {
                                                                                        String prefix = nodeTxt.substring(0, cleanHeader.length());
                                                                                        return HSQTools.levenshtein(prefix, cleanHeader) <= Math.max(1, (int) (cleanHeader.length() * 0.2));
                                                                                    }

                                                                                    return false;
                                                                                }).min(Comparator.comparingInt((TextBlock x) ->
                                                                                {
                                                                                    String nodeTxt = HSQTools.normalizeText(x.text).replaceAll("[^a-z0-9]", "");
                                                                                    return nodeTxt.equals(cleanHeader) ? 0 : 1;
                                                                                }).thenComparingInt(x -> x.y)).orElse(null);

                                                                        if (foundHeader != null)
                                                                        {
                                                                            targetY = foundHeader.y;
                                                                            daThayHeader = true;
                                                                        }
                                                                        else
                                                                        {
                                                                            // 🔥 BỌC THÉP: Vớt Header bằng XML nếu OCR mù do chữ dính chùm
                                                                            String xml = HSQTools.getFlexibleXML();
                                                                            try
                                                                            {
                                                                                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                for (int i = 0; i < nodes.getLength(); i++)
                                                                                {
                                                                                    org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                    String rawText = node.getAttribute("text") + " " + node.getAttribute("content-desc");
                                                                                    String nodeTxt = HSQTools.normalizeText(rawText).replaceAll("[^a-z0-9]", "");

                                                                                    if (!nodeTxt.isEmpty() && (nodeTxt.equals(cleanHeader) || nodeTxt.contains(cleanHeader)))
                                                                                    {
                                                                                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                        // Phải là cái Box không quá bự để tránh bắt nhầm cái Container
                                                                                        if (r != null && r.width() < (widthOfScreen * 0.8) && r.centerY() > 180 && r.centerY() < 2900)
                                                                                        {
                                                                                            targetY = r.centerY();
                                                                                            daThayHeader = true;
                                                                                            updateNotificationContent("OCR Mù! Vớt được Header [" + headerStr + "] trong XML tại Y=" + targetY);
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                            catch (Exception ignored)
                                                                            {
                                                                            }
                                                                        }

                                                                        // =======================================================
                                                                        // 2. KÉO HEADER LÊN ĐỈNH ĐỂ LỘ ĐÁP ÁN (CHỐNG KẸT ĐÁY)
                                                                        // =======================================================
                                                                        if (targetY > 2300 && keoHeader < 2)
                                                                        {
                                                                            updateNotificationContent("Header " + headerStr + " nằm thấp, túm cổ kéo lên đỉnh...");
                                                                            swipe(xs, targetY, xs, 700, 2000);
                                                                            delay(2500);
                                                                            keoHeader++;
                                                                            continue;
                                                                        }
                                                                        else if (targetY > 2300 && keoHeader >= 2)
                                                                        {
                                                                            updateNotificationContent("Header kẹt cứng ở đáy, xả trôi tìm đáp án!");
                                                                        }

                                                                        // =======================================================
                                                                        // 3. TÌM ĐÁP ÁN (CHỈ CHẠY KHI ĐÃ THẤY HEADER)
                                                                        // =======================================================
                                                                        android.graphics.Point finalClickPt = null;

                                                                        if (daThayHeader)
                                                                        {
                                                                            String cleanAnswerForMode = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(answerStr));
                                                                            boolean isNumeric = cleanAnswerForMode.matches("\\d+");

                                                                            android.graphics.Point matrixChoicePt = findClickBlockMatrixChoicePointFromXml(headerStr, answerStr, targetY, currentVisible);
                                                                            if (matrixChoicePt != null)
                                                                            {
                                                                                finalClickPt = matrixChoicePt;
                                                                            }
                                                                            else if (isNumeric)
                                                                            {
                                                                                // CHIẾN THUẬT 1: TÌM THEO TEXT SỐ (Ví dụ click_block {Cau 1~5})
                                                                                TextBlock ansNode = findClickBlockAnswerNode(currentVisible, cleanAnswerForMode, targetY, false);


                                                                                if (ansNode != null)
                                                                                {
                                                                                    finalClickPt = new android.graphics.Point(ansNode.x, ansNode.y);
                                                                                }
                                                                                else
                                                                                {
                                                                                    // CHIẾN THUẬT 2: TÌM SỐ ẨN TRONG HÀNG NGANG (MATRIX KIỂU CŨ)
                                                                                    String xml = HSQTools.getFlexibleXML();
                                                                                    List<android.graphics.Rect> scaleElements = new ArrayList<>();

                                                                                    int preciseAnsY = targetY + 100; // Khởi tạo áng chừng
                                                                                    try
                                                                                    {
                                                                                        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                        org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                        org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                        for (int offset = 0; offset <= 1000; offset += 50)
                                                                                        {
                                                                                            int testY = targetY + offset;
                                                                                            for (int i = 0; i < nodes.getLength(); i++)
                                                                                            {
                                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                                if (r != null && Math.abs(r.centerY() - testY) <= 40 && r.width() >= 10 && r.width() < 300)
                                                                                                {
                                                                                                    scaleElements.add(r);
                                                                                                }
                                                                                            }
                                                                                            if (scaleElements.size() >= 2)
                                                                                            {
                                                                                                preciseAnsY = testY;
                                                                                                break;
                                                                                            }
                                                                                            else
                                                                                            {
                                                                                                scaleElements.clear();
                                                                                            }
                                                                                        }

                                                                                        // 🌟 FIX OFF-BY-ONE BỌC THÉP CHO DÃY SỐ NPS
                                                                                        // Trước tiên, tìm xem trong dàn XML có text CHÍNH XÁC bằng con số đó không
                                                                                        int exactMatchX = -1;
                                                                                        for (int i = 0; i < nodes.getLength(); i++)
                                                                                        {
                                                                                            org.w3c.dom.Element n = (org.w3c.dom.Element) nodes.item(i);
                                                                                            android.graphics.Rect r = HSQTools.parseBoundsFromXml(n.getAttribute("bounds"));
                                                                                            if (r != null && Math.abs(r.centerY() - preciseAnsY) <= 60)
                                                                                            {
                                                                                                String nodeTxt = n.getAttribute("text").trim();
                                                                                                if (nodeTxt.equals(answerStr))
                                                                                                {
                                                                                                    exactMatchX = r.centerX();
                                                                                                    break;
                                                                                                }
                                                                                            }
                                                                                        }

                                                                                        if (exactMatchX != -1)
                                                                                        {
                                                                                            finalClickPt = new android.graphics.Point(exactMatchX, preciseAnsY);
                                                                                        }
                                                                                        else if (!scaleElements.isEmpty())
                                                                                        {
                                                                                            // Gọt trùng lặp (Deduplicate)
                                                                                            List<android.graphics.Rect> uniqueBoxes = new ArrayList<>();
                                                                                            for (android.graphics.Rect r : scaleElements)
                                                                                            {
                                                                                                boolean isDup = false;
                                                                                                for (android.graphics.Rect u : uniqueBoxes)
                                                                                                {
                                                                                                    if (Math.abs(r.centerX() - u.centerX()) < 30)
                                                                                                    {
                                                                                                        isDup = true;
                                                                                                        break;
                                                                                                    }
                                                                                                }
                                                                                                if (!isDup)
                                                                                                    uniqueBoxes.add(r);
                                                                                            }
                                                                                            uniqueBoxes.sort(Comparator.comparingInt(r -> r.centerX()));

                                                                                            int targetClickX = -1;
                                                                                            for (int i = 0; i < nodes.getLength(); i++)
                                                                                            {
                                                                                                org.w3c.dom.Element xNode = (org.w3c.dom.Element) nodes.item(i);
                                                                                                android.graphics.Rect xR = HSQTools.parseBoundsFromXml(xNode.getAttribute("bounds"));
                                                                                                if (xR == null)
                                                                                                    continue;
                                                                                                if (Math.abs(xR.centerY() - preciseAnsY) > 60)
                                                                                                    continue;
                                                                                                String xText = xNode.getAttribute("text").trim();
                                                                                                if (xText.equals(answerStr))
                                                                                                {
                                                                                                    targetClickX = xR.centerX();
                                                                                                    break;
                                                                                                }
                                                                                            }
                                                                                            if (targetClickX > 0)
                                                                                            {
                                                                                                finalClickPt = new android.graphics.Point(targetClickX, preciseAnsY);
                                                                                            }
                                                                                            else
                                                                                            {
                                                                                                int targetIndex = Integer.parseInt(answerStr); // Không trừ 1 vì index 0 = số 0
                                                                                                if (targetIndex >= 0 && targetIndex < uniqueBoxes.size())
                                                                                                    finalClickPt = new android.graphics.Point(uniqueBoxes.get(targetIndex).centerX(), preciseAnsY);
                                                                                            }

                                                                                        }
                                                                                    }
                                                                                    catch (Exception ignored)
                                                                                    {
                                                                                    }
                                                                                }
                                                                            }
                                                                            else
                                                                            {
                                                                                // CHIẾN THUẬT CHỮ (ĐÃ BỌC THÉP CHỐNG CẮN NGƯỢC)

                                                                                TextBlock ansNode = findClickBlockAnswerNode(currentVisible, cleanAnswerForMode, targetY, true);
                                                                                if (ansNode != null)
                                                                                {
                                                                                    int finalClickX = ansNode.x;
                                                                                    int finalClickY = ansNode.y;

                                                                                    if (finalClickX > 400 && finalClickX < 1200)
                                                                                    {
                                                                                        String xml = HSQTools.getFlexibleXML();
                                                                                        try
                                                                                        {
                                                                                            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                            for (int i = 0; i < nodes.getLength(); i++)
                                                                                            {
                                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                                String rawText = node.getAttribute("text") + " " + node.getAttribute("content-desc");
                                                                                                String xmlText = HSQTools.normalizeText(rawText).replaceAll("[^a-z0-9]", "");

                                                                                                if (!xmlText.isEmpty() && isClickAnswerMatchOcrFriendly(xmlText, cleanAnswerForMode, true))
                                                                                                {
                                                                                                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                                    if (r != null && Math.abs(r.centerY() - finalClickY) <= 40)
                                                                                                    {
                                                                                                        if (r.width() < (widthOfScreen * 0.5) && r.centerX() > 0)
                                                                                                        {
                                                                                                            finalClickX = r.centerX();
                                                                                                        }
                                                                                                        else if (r.width() >= (widthOfScreen * 0.5))
                                                                                                        {
                                                                                                            finalClickX = r.left + 80;
                                                                                                        }
                                                                                                        break;
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        catch (Exception ignored)
                                                                                        {
                                                                                        }
                                                                                    }
                                                                                    finalClickPt = new android.graphics.Point(finalClickX, finalClickY);
                                                                                }
                                                                            }
                                                                        } // KẾT THÚC KHỐI `if (daThayHeader)`

                                                                        // =======================================================
                                                                        // 4. CHỐT CHẶN KIỂM TRA LỖI & THUẬT TOÁN VUỐT 2 CHIỀU
                                                                        // =======================================================
                                                                        if (finalClickPt == null)
                                                                        {
                                                                            if (HSQTools.areAlmostSame(tempCompare, currentVisible, 20))
                                                                            {
                                                                                if (vuotLenLai == 0)
                                                                                {
                                                                                    vuotLenLai = 1;
                                                                                    updateNotificationContent("Chạm đáy! Quay xe cuộn ngược lên tìm...");
                                                                                }
                                                                                else
                                                                                {
                                                                                    handleActionFailure(
                                                                                            "Block_NotFound", step, currentVisible,
                                                                                            "Lỗi Block: Tao đã cuộn nát cái form này từ đỉnh xuống đáy rồi ngược lại mà đéo thấy đáp án [" + answerStr + "] của câu hỏi [" + headerStr + "] đâu!, kiểm tra lại rule",
                                                                                            splitTextAnswer[1]
                                                                                    );
                                                                                    tempTextAnswer = textAnswer;
                                                                                    continue stateMachine;
                                                                                }
                                                                            }

                                                                            tempCompare = new ArrayList<>(currentVisible);

                                                                            if (vuotLenLai == 0)
                                                                            {
                                                                                // CHIỀU ĐI XUỐNG
                                                                                if (targetY > 350)
                                                                                {
                                                                                    updateNotificationContent("Đẩy Header lên đỉnh để lộ đáp án...");
                                                                                    swipe(xs, targetY, xs, 250, 2000);
                                                                                }
                                                                                else
                                                                                {
                                                                                    updateNotificationContent("Cuộn xuống tìm...");
                                                                                    swipe(xs, ysBot, xs, ysTop, 2000);
                                                                                }
                                                                                delay(2000);
                                                                            }
                                                                            else
                                                                            {
                                                                                // CHIỀU ĐI LÊN
                                                                                updateNotificationContent("Cuộn ngược lên tìm...");
                                                                                swipe(xs, ysTop, xs, ysBot, 2000);
                                                                            }

                                                                            delay(2500);
                                                                            continue;
                                                                        }

                                                                        // =======================================================
                                                                        // 5. THỰC THI CLICK (NẾU TÌM THẤY)
                                                                        // =======================================================
                                                                        if (finalClickPt != null)
                                                                        {
                                                                            click(finalClickPt.x, finalClickPt.y, false);
                                                                            delay(2000);
                                                                            break;
                                                                        }
                                                                    }
                                                                }
                                                                break;
                                                            }
                                                        }
                                                        else if (step.contains("auto_choose"))
                                                        {
                                                            Matcher match = Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                            if (match.find() && match.group(1).contains("~"))
                                                            {
                                                                String[] parts = match.group(1).split("~");
                                                                String headerStr = parts[0];
                                                                String answerValue = parts[1];

                                                                updateNotificationContent("🤖 Auto Choose: Cố gắng chọn [" + answerValue + "]");
                                                                String normAnswer = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(answerValue));

                                                                // =======================================================
                                                                // 🎯 GIAI ĐOẠN 1: TÌM TRỰC TIẾP TRÊN MÀN HÌNH (Dành cho Radio/Checkbox hiển thị sẵn)
                                                                // =======================================================
                                                                List<TextBlock> currentVisible = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                                TextBlock directTarget = currentVisible.stream()
                                                                        .filter(x ->
                                                                        {
                                                                            String txt = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text));
                                                                            return isClickAnswerMatchOcrFriendly(txt, normAnswer, true);
                                                                        })
                                                                        .min(Comparator.comparingInt(x -> Math.abs(x.x - xCenter))) // Ưu tiên gần tâm
                                                                        .orElse(null);

                                                                if (directTarget != null)
                                                                {
                                                                    updateNotificationContent("Thấy ngay đáp án trên màn hình! Click: " + directTarget.text);
                                                                    click(directTarget.x, directTarget.y, false);
                                                                    delay(2000);
                                                                }
                                                                else
                                                                {
                                                                    // =======================================================
                                                                    // 🎯 GIAI ĐOẠN 2: KHÔNG THẤY ĐÁP ÁN -> TRUY TÌM HEADER ĐỂ MỞ DROPDOWN/INPUT
                                                                    // =======================================================
                                                                    updateNotificationContent("Không thấy đáp án. Đang tìm Câu hỏi để mở Dropdown...");
                                                                    String normHeader = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(headerStr));
                                                                    TextBlock headerTarget = currentVisible.stream()
                                                                            .filter(x ->
                                                                            {
                                                                                String txt = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text));
                                                                                return txt.equals(normHeader) || txt.contains(normHeader) || normHeader.contains(txt) || HSQTools.levenshtein(txt, normHeader) <= (normHeader.length() * 0.15);
                                                                            })
                                                                            .min(Comparator.comparingInt(x -> x.y)) // Ưu tiên cao nhất
                                                                            .orElse(null);

                                                                    if (headerTarget != null)
                                                                    {
                                                                        // Thấy Header rồi! Ta sẽ chọt vào tọa độ CỦA THẰNG CON NẰM NGAY DƯỚI NÓ
                                                                        // Bằng cách dùng ASBL hoặc tính toán tọa độ
                                                                        AccessibilityNodeInfo root = ASBLBridgeService.asblService.getRootInActiveWindow();
                                                                        android.graphics.Point dropPt = null;

                                                                        if (root != null)
                                                                        {
                                                                            // Tái sử dụng hàm tìm Dropdown sếp đã viết
                                                                            dropPt = findDropdownNearByASBL(root, headerTarget.x, headerTarget.y, "");
                                                                            // Nếu mù, tái sử dụng hàm tìm Input
                                                                            if (dropPt == null)
                                                                            {
                                                                                android.graphics.Rect inputRect = findInputNearYByASBL(root, headerTarget.y);
                                                                                if (inputRect != null)
                                                                                    dropPt = new android.graphics.Point(inputRect.centerX(), inputRect.centerY());
                                                                            }
                                                                            root.recycle();
                                                                        }

                                                                        // Nếu ASBL mù, ta dùng chọt mù (Chọt vào vị trí thấp hơn Header 120px)
                                                                        if (dropPt == null)
                                                                        {
                                                                            dropPt = new android.graphics.Point(headerTarget.x, headerTarget.y + 120);
                                                                        }

                                                                        updateNotificationContent("Chọt vào ô bên dưới Header: " + dropPt.x + ", " + dropPt.y);
                                                                        click(dropPt.x, dropPt.y, false);
                                                                        delay(3000); // Chờ list xổ ra hoặc bàn phím nảy lên

                                                                        // =======================================================
                                                                        // 🎯 GIAI ĐOẠN 3: PHÂN LOẠI UI BẰNG KEYBOARD
                                                                        // =======================================================
                                                                        if (HSQTools.isKeyboardVisibleSmart())
                                                                        {
                                                                            // Bàn phím nảy lên -> Đây là ô Input (Text Box)
                                                                            updateNotificationContent("Đã mở Keyboard -> Dùng lệnh Gõ phím");
                                                                            clearAllText();
                                                                            delay(1000);
                                                                            inputText(answerValue, null, true);
                                                                            delay(2000);
                                                                        }
                                                                        else
                                                                        {
                                                                            // Bàn phím không lên -> List Dropdown vừa được xổ ra
                                                                            updateNotificationContent("Đã xổ Dropdown -> Tìm đáp án lần 2");
                                                                            List<TextBlock> afterOpenVisible = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                                            phoneScreen.addAll(afterOpenVisible);
                                                                            phoneScreen.add(new TextBlock("Đã vuốt xuống", 0, 0));

                                                                            TextBlock dropdownTarget = afterOpenVisible.stream()
                                                                                    .filter(x ->
                                                                                    {
                                                                                        String txt = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text));
                                                                                        return isClickAnswerMatchOcrFriendly(txt, normAnswer, true);
                                                                                    })
                                                                                    .min(Comparator.comparingInt(x -> Math.abs(x.x - xCenter)))
                                                                                    .orElse(null);

                                                                            if (dropdownTarget != null)
                                                                            {
                                                                                click(dropdownTarget.x, dropdownTarget.y, false);
                                                                                delay(2000);
                                                                            }
                                                                        }
                                                                    }

                                                                    // =======================================================
                                                                    // 🎯 GIAI ĐOẠN 4: ĐẦU HÀNG, ĐẨY CHO SERVER AI
                                                                    // =======================================================
                                                                    // Vuốt lên vuốt xuống, tìm đủ kiểu mà mù hết -> Nhả ra để Gemini/LocalServer phân tích ảnh!
                                                                    handleActionFailure(
                                                                            "Auto_Choose", step, currentVisible,
                                                                            "Lệnh auto_choose thất bại do không tìm thấy đáp án [" + answerValue + "] hoặc tiêu đề [" + headerStr + "]. Mày hãy đọc ảnh và xuất lại các lệnh cơ bản (clicktotext, dropdown, input) cho tao.",
                                                                            splitTextAnswer[1]
                                                                    );
                                                                    tempTextAnswer = textAnswer;
                                                                    continue stateMachine;
                                                                }
                                                            }
                                                        }
                                                        else if (step.contains("slider"))
                                                        {
                                                            Matcher match = Pattern.compile("\\{([^~]+)~([^}]+)\\}").matcher(step);
                                                            if (match.find())
                                                            {
                                                                String labelToFind = match.group(1).trim();
                                                                String percentStr = match.group(2).trim();

                                                                int percent = 50; // Mặc định là 50% nếu AI ngáo
                                                                try
                                                                {
                                                                    percent = Integer.parseInt(percentStr.replaceAll("[^0-9]", ""));
                                                                }
                                                                catch (Exception ignored)
                                                                {
                                                                }

                                                                List<TextBlock> resultScreen = clickSlider(labelToFind, percent);
                                                                if (resultScreen != null)
                                                                {
                                                                    handleActionFailure(
                                                                            "Slider_Error", step, resultScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()),
                                                                            "Đéo tìm thấy thanh trượt cho câu [" + labelToFind + "]", splitTextAnswer[1]
                                                                    );
                                                                    tempTextAnswer = textAnswer;
                                                                    continue stateMachine;
                                                                }
                                                            }
                                                        }
                                                        else if (step.contains("click_point"))
                                                        {
                                                            Matcher match = Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                            if (match.find())
                                                            {
                                                                String pts = match.group(1);
                                                                int pts_x = Integer.parseInt(pts.split(Pattern.quote(","))[0]);
                                                                int pts_y = Integer.parseInt(pts.split(Pattern.quote(","))[1]);
                                                                click(pts_x, pts_y, false);
                                                            }
                                                        }
                                                        else
                                                        {
                                                            if (step.length() > 2)
                                                            {
                                                                if (AINguL > 0)
                                                                {
                                                                    if (createAgain > 0)
                                                                    {
                                                                        HSQTools.sendTelegramAlert(deviceID, step, idTelegram);
                                                                        currentState = STATE_GET_ANSWER;
                                                                        continue stateMachine;
                                                                    }
                                                                    createAgain++;
                                                                    createNewChatGemByApi(customAgentRule, true);
                                                                    currentState = STATE_GET_ANSWER;
                                                                    continue stateMachine;
                                                                }
                                                                else
                                                                {
                                                                    AINguL++;
                                                                    screenBegin = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                                    textAnswer = sendChatToGemByApi("sai định dạng câu trả lời, hãy nhớ lại các rule liên quan clicktotext, clickbutton, swipe, dropdown, dragdrop, matrix_dropdown, input, accordion và các lưu ý kèm theo, trả lời lại ngắn gọn không giải thích đúng rule đúng format");
                                                                    currentState = STATE_ANSWER_OK;
                                                                    continue stateMachine;
                                                                }
                                                            }
                                                        }
                                                        delay(3000);
                                                    }
                                                }
                                                catch (Exception ex)
                                                {
                                                    writeSerLogs("Lỗi tại step " + step + ": " + ex.getMessage());
                                                    updateNotificationContent("Lỗi tại step " + step);

                                                    if (AINguL > 0)
                                                    {
                                                        if (createAgain > 0)
                                                        {
                                                            HSQTools.sendTelegramAlert(deviceID, step, idTelegram);
                                                            delay(2000);
                                                            updateNotificationContent("Lỗi step");
                                                            delay(5000);
                                                            List<HSQTools.TextBlock> checkUserAct1 = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                            while (true)
                                                            {
                                                                delay(180000);
                                                                List<HSQTools.TextBlock> checkUserAct2 = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                                if (!HSQTools.areAlmostSame(checkUserAct1, checkUserAct2, 20))
                                                                {
                                                                    break;
                                                                }
                                                            }
                                                            AINguL = 0;
                                                            createAgain = 0;
                                                            currentState = STATE_GET_ANSWER;
                                                            continue;
                                                        }
                                                        createAgain++;
                                                        createNewChatGemByApi(customAgentRule, true);
                                                        currentState = STATE_GET_ANSWER;
                                                    }
                                                    else
                                                    {
                                                        AINguL++;
                                                        screenBegin = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                        textAnswer = sendChatToGemByApi("lỗi phân tích cú pháp, kiểm tra lại các rule và trả lời lại câu trên");
                                                        currentState = STATE_ANSWER_OK;
                                                    }
                                                    continue;
                                                }
                                                tempSwipeCount = 0;

                                                if (checkGDKS == 15 || checkGDKS == 10)
                                                {
                                                    // Tương đương goto lamprofile; -> Phá vỡ vòng lặp buserLoop để trồi lên lamProfileLoop
                                                    continue lamProfileLoop;
                                                }
                                                delay(10000);
                                            }
                                            //endregion

                                            break stateMachine; // Kết thúc stateMachine
                                    }
                                }
                            }
                        } // End buserLoop
                    }
                    delay(1000);

                }

            }
        }
        catch (Exception ex)
        {
            delay(1000);
        }
    }

    public static void delayRandom(int time1, int time2)
    {
        Random random = new Random();
        int randomNum = random.nextInt(time2 - time1) + time1;
        delay(randomNum * 1000);
    }

    private void loadControl()
    {
        String isSuccess;
        int i = 0;
        while (true)//load control từ server
        {
            i++;
            updateNotificationContent("Load control " + i);
            JSONObject bodyPost = new JSONObject();
            try
            {
                bodyPost.put("api_key", apiRun);
                bodyPost.put("device_id", deviceID);
            }
            catch (Exception ignored)
            {
            }
            JSONObject controlserver = HSQHttps.postRequest("https://quaykute.id.vn/api/user/control", bodyPost.toString(), JSONObject.class, true);
            try
            {
                if (controlserver != null)
                {
                    isSuccess = controlserver.getString("status");
                    if (isSuccess.contains("success"))
                    {
                        JSONObject control = controlserver.getJSONObject("data");
                        apkVersion = control.getInt("apkVersion");
                        localServerIp = control.getString("localServerIp");
                        if (!upDateTool())
                        {
                            continue;
                        }
                        idTelegram = control.getString("idTelegram");
                        remotePromtVersion = control.getInt("remotePromtVersion");
                        if (!updatePromt())
                        {
                            continue; // Nếu có lệnh update mà tải lỗi thì bắt vòng lặp load lại
                        }
                        AIWebSite1 = control.getString("AIWebSite1");
                        AIApiKey1 = control.getString("AIApiKey1");
                        aiModel1 = control.getString("aiModel1");
                        AIWebSite = AIWebSite1;
                        AIApiKey = AIApiKey1;
                        aiModel = aiModel1;
                        AIWebSite2 = control.getString("AIWebSite2");
                        AIApiKey2 = control.getString("AIApiKey2");
                        aiModel2 = control.getString("aiModel2");
                        AIProxyEnabled = control.optBoolean("AIProxyEnabled", false);
                        AIProxyUrl = control.optString("AIProxyUrl", "https://quaykute.id.vn");
                        break;
                    }
                }
            }
            catch (Exception ignored)
            {
            }
            HSQTools.delay(10000);
        }
    }

    @SuppressLint("SdCardPath")
    private boolean upDateTool()
    {
        if (VCode < apkVersion)
        {
            StartAuto.isUpdating = true;
            updateNotificationContent("down apk " + apkVersion);
            beginInstall:
            while (true)
            {
                int tryReinstall = 1;
                String linkDownLoad = "https://quaykute.id.vn/hihi/SHTools" + apkVersion + ".bin";
                String filePath = "/sdcard/Download/SHTools.apk";
                File fileDeLuu = new File(filePath);

                if (HSQHttps.downloadFile(linkDownLoad, fileDeLuu, false))
                {
                    if (fileDeLuu.exists())
                    {
                        long fileSizeInBytes = fileDeLuu.length();
                        double fileSizeInKB = (double) fileSizeInBytes / 1024;
                        double fileSizeInMB = fileSizeInKB / 1024;
                        if (fileSizeInMB > 7)
                        {
                            AppInstaller.installApk(HSQConfig.getContext(), filePath);
                            delay(2000);
                            while (true)
                            {
                                int checkInstall = ASBLBridgeService.findMultiTextDesWindow(60, true, true, true, false, "install", "there was a problem parsing the package");
                                if (checkInstall == 2 || checkInstall == 0)
                                {
                                    updateNotificationContent("Lỗi cài apk " + tryReinstall);
                                    if (tryReinstall < 3)
                                    {
                                        delay(5000);
                                        tryReinstall++;
                                        continue;
                                    }
                                    delay(180000);
                                    continue beginInstall;
                                }
                                else
                                {
                                    break;
                                }
                            }
                            delay(3000);
                            while (true)
                            {
                                int checkInstall = ASBLBridgeService.findMultiTextDesWindow(360, true, true, true, false, "decline", "done");
                                if (checkInstall == 1)
                                {
                                    delay(5000);
                                }
                                else
                                {
                                    return true;
                                }
                            }
                        }
                    }
                }
                else
                {
                    if (HSQHttps.isServerReachable("http://" + localServerIp + ":5000"))
                    {
                        linkDownLoad = "http://" + localServerIp + ":5000/download/apk/SHTools" + apkVersion + ".bin";
                    }
                    else
                    {
                        linkDownLoad = "http://quay.hopto.org:5000/download/apk/SHTools" + apkVersion + ".bin";
                    }
                    if (HSQHttps.downloadFile(linkDownLoad, fileDeLuu, false))
                    {
                        if (fileDeLuu.exists())
                        {
                            long fileSizeInBytes = fileDeLuu.length();
                            double fileSizeInKB = (double) fileSizeInBytes / 1024;
                            double fileSizeInMB = fileSizeInKB / 1024;
                            if (fileSizeInMB > 7)
                            {
                                AppInstaller.installApk(HSQConfig.getContext(), filePath);
                                delay(2000);
                                while (true)
                                {
                                    int checkInstall = ASBLBridgeService.findMultiTextDesWindow(60, true, true, true, false, "install", "there was a problem parsing the package");
                                    if (checkInstall == 2 || checkInstall == 0)
                                    {
                                        updateNotificationContent("Lỗi cài apk " + tryReinstall);
                                        if (tryReinstall < 3)
                                        {
                                            delay(5000);
                                            tryReinstall++;
                                            continue;
                                        }
                                        delay(180000);
                                        continue beginInstall;
                                    }
                                    else
                                    {
                                        break;
                                    }
                                }
                                delay(3000);

                                while (true)
                                {
                                    int checkInstall = ASBLBridgeService.findMultiTextDesWindow(360, true, true, true, false, "decline", "done");
                                    if (checkInstall == 1)
                                    {
                                        delay(5000);
                                    }
                                    else
                                    {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        updateNotificationContent("down apk dp" + apkVersion);
                        String linkDownLoadDP = "http://quaykute.id.vn/hihi/SHTools" + apkVersion + ".bin";
                        if (HSQHttps.downloadFile(linkDownLoadDP, fileDeLuu, false))
                        {
                            if (fileDeLuu.exists())
                            {
                                long fileSizeInBytes = fileDeLuu.length();
                                double fileSizeInKB = (double) fileSizeInBytes / 1024;
                                double fileSizeInMB = fileSizeInKB / 1024;
                                if (fileSizeInMB > 7)
                                {
                                    AppInstaller.installApk(HSQConfig.getContext(), filePath);
                                    delay(2000);
                                    while (true)
                                    {
                                        int checkInstall = ASBLBridgeService.findMultiTextDesWindow(60, true, true, true, false, "install", "there was a problem parsing the package");
                                        if (checkInstall == 2 || checkInstall == 0)
                                        {
                                            updateNotificationContent("Lỗi cài apk " + tryReinstall);
                                            if (tryReinstall < 3)
                                            {
                                                delay(5000);
                                                tryReinstall++;
                                                continue;
                                            }
                                            delay(180000);
                                            continue beginInstall;
                                        }
                                        else
                                        {
                                            break;
                                        }
                                    }
                                    delay(3000);

                                    while (true)
                                    {
                                        int checkInstall = ASBLBridgeService.findMultiTextDesWindow(360, true, true, true, false, "decline", "done");
                                        if (checkInstall == 1)
                                        {
                                            delay(5000);
                                        }
                                        else
                                        {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            updateNotificationContent("không thể download");
                            HSQTools.delay(10000);
                            return false;
                        }
                    }
                }
                return false;
            }
        }
        return true;
    }

    @SuppressLint("SdCardPath")
    private boolean updatePromt()
    {
        // 1. Lấy version prompt hiện tại đang lưu trong máy (mặc định chưa có là 0)
        android.content.SharedPreferences prefs = com.quayquay.hsq.tools.HSQConfig.getContext()
                .getSharedPreferences("QQ_PREFS_DATA", android.content.Context.MODE_PRIVATE);
        int localPromtVersion = prefs.getInt("PROMT_VERSION", 0);

        // 2. Nếu máy đang chạy bản cũ hơn bản trên server -> Tiến hành lôi về
        if (localPromtVersion < remotePromtVersion)
        {
            updateNotificationContent("down promt v" + remotePromtVersion);

            // Đảm bảo thư mục lưu trữ luôn tồn tại
            HSQFileHelper.createFolder("/sdcard/Servey");
            String filePath = "/sdcard/Servey/PromtGem.txt";
            File fileDeLuu = new File(filePath);

            while (true)
            {
                String linkDownLoad = "https://quaykute.id.vn/hihi/PromtGem.txt";
                if (HSQHttps.downloadFile(linkDownLoad, fileDeLuu, false))
                {
                    if (fileDeLuu.exists() && fileDeLuu.length() > 500)
                    { // Đảm bảo file tải về chứa chữ thật (>500 bytes)
                        // Tải thành công -> Khóa cứng mốc version mới vào SharedPreferences
                        prefs.edit().putInt("PROMT_VERSION", remotePromtVersion).apply();
                        updateNotificationContent("Đã update Promt v" + remotePromtVersion);
                        return true;
                    }
                }
                else
                {
                    // Ưu tiên Server mạng nội bộ (Local IP)
                    if (HSQHttps.isServerReachable("http://" + localServerIp + ":5000", false))
                    {
                        linkDownLoad = "http://" + localServerIp + ":5000/download/apk/PromtGem.txt";
                    }
                    else
                    {
                        linkDownLoad = "http://quay.hopto.org:5000/download/apk/PromtGem.txt";
                    }

                    if (HSQHttps.downloadFile(linkDownLoad, fileDeLuu, false))
                    {
                        if (fileDeLuu.exists() && fileDeLuu.length() > 500)
                        { // Đảm bảo file tải về chứa chữ thật (>500 bytes)
                            // Tải thành công -> Khóa cứng mốc version mới vào SharedPreferences
                            prefs.edit().putInt("PROMT_VERSION", remotePromtVersion).apply();
                            updateNotificationContent("Đã update Promt v" + remotePromtVersion);
                            delay(2000);
                            return true;
                        }
                    }
                    else
                    {
                        updateNotificationContent("down promt dp v" + remotePromtVersion);
                        // LINK DỰ PHÒNG CHÍNH THỨC NGOÀI INTERNET
                        String linkDownLoadDP = "http://quaykute.id.vn/hihi/PromtGem.txt";
                        if (HSQHttps.downloadFile(linkDownLoadDP, fileDeLuu, false))
                        {
                            if (fileDeLuu.exists() && fileDeLuu.length() > 500)
                            {
                                prefs.edit().putInt("PROMT_VERSION", remotePromtVersion).apply();
                                updateNotificationContent("Đã update Promt v" + remotePromtVersion);
                                delay(2000);
                                return true;
                            }
                        }
                        else
                        {
                            updateNotificationContent("Lỗi tải Promt từ xa!");
                            HSQTools.delay(10000);
                            return false;
                        }
                    }
                }
                return false;
            }
        }
        return true; // Nếu bằng version nhau thì phớt lờ, coi như đã up-to-date
    }

    @SuppressLint("SdCardPath")
    private boolean updateProfile()
    {
        updateNotificationContent("down PRF");

        // Đảm bảo thư mục lưu trữ luôn tồn tại
        HSQFileHelper.createFolder("/sdcard/Servey");
        File fileDeLuu = new File(pathInfoProfileSaved);

        while (true)
        {
            String linkDownLoad = "https://quaykute.id.vn/servey_profile/sv_" + deviceID + ".json";
            if (HSQHttps.downloadFile(linkDownLoad, fileDeLuu, false))
            {
                if (fileDeLuu.exists() && fileDeLuu.length() > 500)
                {
                    delay(2000);
                    return true;
                }
            }
            else
            {
                updateNotificationContent("Lỗi tải profile từ xa!");
                HSQTools.delay(10000);
            }
        }
    }

    private int findBestBituroAccountPoint()
    {
        Map<Integer, Integer> scoreByPoint = new HashMap<>();
        Map<Integer, Integer> minYByPoint = new HashMap<>();
        Map<Integer, String> sampleByPoint = new HashMap<>();
        int topLimit = heightOfScreen > 0 ? Math.max(420, Math.min(800, heightOfScreen / 4)) : 800;

        for (int scan = 0; scan < 5; scan++)
        {
            List<HSQTools.TextBlock> blocks = getCheckAnswerSmart();
            if (blocks != null)
            {
                for (HSQTools.TextBlock block : blocks)
                {
                    if (block == null || block.text == null) continue;
                    if (block.y <= 45 || block.y >= topLimit) continue;

                    Integer point = parseBituroPointCandidate(block.text);
                    if (point == null || point <= 0 || point > 10000000) continue;

                    int score = 10;
                    score += block.y < (topLimit * 0.55f) ? 8 : 3;
                    if (point >= 10) score += 4;
                    else score -= 6;
                    if (point >= 100) score += 5;

                    scoreByPoint.put(point, scoreByPoint.containsKey(point) ? scoreByPoint.get(point) + score : score);
                    int oldMinY = minYByPoint.containsKey(point) ? minYByPoint.get(point) : Integer.MAX_VALUE;
                    minYByPoint.put(point, Math.min(oldMinY, block.y));
                    if (!sampleByPoint.containsKey(point))
                    {
                        sampleByPoint.put(point, block.text + " @" + block.x + "," + block.y);
                    }
                }
            }
            delay(650);
        }

        if (scoreByPoint.isEmpty()) return -1;

        boolean hasPointAbove9 = false;
        for (Integer point : scoreByPoint.keySet())
        {
            if (point != null && point >= 10)
            {
                hasPointAbove9 = true;
                break;
            }
        }

        int bestPoint = -1;
        int bestScore = Integer.MIN_VALUE;
        StringBuilder debug = new StringBuilder("Account point candidates: ");

        for (Map.Entry<Integer, Integer> entry : scoreByPoint.entrySet())
        {
            int point = entry.getKey();
            int score = entry.getValue();
            if (hasPointAbove9 && point < 10) score -= 1000;
            score += Math.min(point, 500000) / 100;
            score -= minYByPoint.containsKey(point) ? (minYByPoint.get(point) / 100) : 0;

            debug.append(point)
                    .append(" pts(score=").append(score)
                    .append(", sample=").append(sampleByPoint.get(point))
                    .append("); ");

            if (score > bestScore || (score == bestScore && point > bestPoint))
            {
                bestScore = score;
                bestPoint = point;
            }
        }

        Log.d("TEST_TREO", debug.toString());
        writeSerLogs(debug + "=> chọn " + bestPoint + " pts");
        return bestPoint;
    }

    private Integer parseBituroPointCandidate(String text)
    {
        if (text == null) return null;
        String rawText = text.toLowerCase(Locale.US).trim();
        Matcher matcher = Pattern.compile("(^|[^0-9a-z])([0-9oli][0-9oli,.\\s]{0,14})\\s*(pts|points)\\b", Pattern.CASE_INSENSITIVE).matcher(rawText);
        Integer best = null;

        while (matcher.find())
        {
            String prefix = matcher.group(2).trim();
            if (!prefix.matches("^[0-9oli,.\\s]+$")) continue;

            String cleanNumStr = prefix
                    .replace("o", "0")
                    .replace("l", "1")
                    .replace("i", "1")
                    .replaceAll("[^0-9]", "");

            if (cleanNumStr.isEmpty()) continue;
            try
            {
                int point = Integer.parseInt(cleanNumStr);
                if (point > 0 && (best == null || point > best)) best = point;
            }
            catch (Exception ignored) {}
        }
        return best;
    }
    private void updatePoint(String points)
    {
        while(true) {
            JSONObject bodyPost = new JSONObject();
            try
            {
                bodyPost.put("api_key", apiRun);
                bodyPost.put("device_id", deviceID);
                bodyPost.put("points", points);
            }
            catch (Exception ignored)
            {
            }
            String upPoints = HSQHttps.postRequest("https://quaykute.id.vn/api/user/device-points", bodyPost.toString(), String.class, true);
            if(upPoints.contains("success")) {
                return;
            }
            delay(5000);
        }
    }

    public static void openDeepLink(String uri, String packageName)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        intent.setPackage(packageName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        HSQConfig.getContext().startActivity(intent);
    }

    @Override
    public int getStartCommandFlags()
    {
        return android.app.Service.START_NOT_STICKY;
    }

    @Override
    public boolean shouldObserveVolumeChanges()
    {
        return true;
    }

    @Override
    public boolean onPauseServiceByVolume()
    {
        return true;
    }

    @Override
    public void onVolumeChangedForServiceStop()
    {
        if (isRestartingFromVolume) return;
        isRestartingFromVolume = true;
        updateNotificationContent("Dang restart tool...");

        new Thread(null, () -> {
            try
            {
                restartWholeToolFromVolume();
            }
            catch (Exception e)
            {
                Log.e("TEST_TREO", "Volume restart failed", e);
                isRestartingFromVolume = false;
            }
        }, "SHToolsVolumeRestart", 4 * 1024 * 1024).start();
    }

    private void restartWholeToolFromVolume()
    {
        isStop = true;
        isToolRunning = false;

        flushAiHistory("volume_restart", true);

        try
        {
            RemoteStreamManager.releaseInstance();
        }
        catch (Exception e)
        {
            Log.w("TEST_TREO", "release remote stream failed | " + e.getMessage());
        }

        try
        {
            stopService(new Intent(this, com.quayquay.shtools.services.StreamService.class));
        }
        catch (Exception ignored)
        {
        }

        SharedPreferences runtimePrefs = getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE);
        runtimePrefs.edit()
                .putBoolean("isForceStopped", false)
                .putBoolean(PREF_RESTART_FROM_VOLUME, true)
                .putBoolean(PREF_SUPPRESS_AUTOSTART_AFTER_VOLUME, true)
                .putLong(PREF_SUPPRESS_ASBL_AUTO_OPEN_UNTIL, System.currentTimeMillis() + ASBL_AUTO_OPEN_SUPPRESS_MS)
                .putString(PREF_SAVED_DEVICE_ID, StartAuto.deviceID)
                .apply();

        scheduleMainActivityRestartFromVolume();

        try
        {
            stopForeground(true);
        }
        catch (Exception ignored)
        {
        }
        stopSelf();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try
            {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
            finally
            {
                System.exit(0);
            }
        }, 350);
    }

    private void requestScreencastPermissionAndStop(Context context, SharedPreferences runtimePrefs)
    {
        isStop = true;
        isToolRunning = false;
        updateNotificationContent("Thieu quyen screencast - mo app cap lai");

        runtimePrefs.edit()
                .putBoolean("isForceStopped", false)
                .putBoolean(PREF_RESTART_FROM_VOLUME, true)
                .putBoolean(PREF_SUPPRESS_AUTOSTART_AFTER_VOLUME, false)
                .putLong(PREF_SUPPRESS_ASBL_AUTO_OPEN_UNTIL, System.currentTimeMillis() + ASBL_AUTO_OPEN_SUPPRESS_MS)
                .putString(PREF_SAVED_DEVICE_ID, StartAuto.deviceID)
                .apply();

        try
        {
            RemoteStreamManager.releaseInstance();
        }
        catch (Exception ignored)
        {
        }

        Intent restartIntent = new Intent(context, MainActivity.class);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        restartIntent.putExtra(PREF_RESTART_FROM_VOLUME, true);
        restartIntent.putExtra(PREF_SUPPRESS_AUTOSTART_AFTER_VOLUME, false);
        try
        {
            context.startActivity(restartIntent);
        }
        catch (Exception e)
        {
            Log.w("TEST_TREO", "open screencast permission activity failed | " + e.getMessage());
            scheduleMainActivityRestartFromVolume();
        }

        stopSelf();
    }

    private void scheduleMainActivityRestartFromVolume()
    {
        Intent restartIntent = new Intent(this, MainActivity.class);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        restartIntent.putExtra(PREF_RESTART_FROM_VOLUME, true);
        restartIntent.putExtra(PREF_SUPPRESS_AUTOSTART_AFTER_VOLUME, true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, VOLUME_RESTART_REQUEST_CODE, restartIntent, flags);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long triggerAt = System.currentTimeMillis() + 1200;

        try
        {
            if (alarmManager != null)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                }
                else
                {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                }
            }
            else
            {
                pendingIntent.send();
            }
        }
        catch (Exception e)
        {
            Log.e("TEST_TREO", "schedule restart failed", e);
            try
            {
                if (alarmManager != null)
                {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                }
                else
                {
                    pendingIntent.send();
                }
            }
            catch (Exception ignored)
            {
            }
        }
    }

    @Override
    public void onDestroy()
    {
        flushAiHistory("destroy", true);
        try
        {
            RemoteStreamManager.releaseInstance();
        }
        catch (Exception ignored)
        {
        }
        super.onDestroy();
        isStop = true;
    }

    private void flushAiHistory(String reason, boolean releaseRam)
    {
        if (AIHelper == null) return;
        try
        {
            AIHelper.saveHistory();
            Log.d("TEST_TREO", "AI history saved | reason=" + reason);
        }
        catch (Exception e)
        {
            Log.e("TEST_TREO", "AI history save failed | reason=" + reason + " | " + e.getMessage());
        }

        if (releaseRam)
        {
            try
            {
                AIHelper.freeRam();
            }
            catch (Exception ignored)
            {
            }
        }
    }

    @SuppressLint("SdCardPath")
    private void writeSerLogs(String textLogs)
    {
        // 1. Đảm bảo thư mục tồn tại
        HSQFileHelper.createFolder("/sdcard/Servey");
        @SuppressLint("SdCardPath") String logFilePath = "/sdcard/Servey/servey_logs.txt";

        // 2. Lấy thời gian hiện tại format theo đúng ý bạn
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        // 3. Tạo chuỗi 50 dấu gạch ngang (tương đương new string('-', 50) trong C#)
        StringBuilder dashes = new StringBuilder();
        for (int i = 0; i < 50; i++)
        {
            dashes.append("-");
        }

        // 4. Gộp toàn bộ nội dung log lại thành 1 chuỗi
        String logContent = "Date: " + currentTime + "\n"
                + "Step Info: " + textLogs + "\n"
                + dashes;

        // 5. Ghi nối vào file bằng hàm siêu tốc của bạn
        HSQFileHelper.writetoFile(logContent, logFilePath);
    }

    // =========================================================
    // 1. TẠO MỚI HOẶC LOAD CHAT
    // =========================================================
    private void createNewChatGemByApi(String customAgentRule, boolean deleteOldChat)
    {
        updateNotificationContent("Đang thiết lập Context API...");
        Log.d("TEST_TREO", "createNewChatGemByApi(deleteOldChat=" + deleteOldChat + ")");

        if (deleteOldChat)
        {
            Log.d("TEST_TREO", "createNewChatGemByApi -> gọi deleteChat()");
            AIHelper.deleteChat(); // Xóa sạch lịch sử
        }

        AIHelper.loadOrCreateChat(customAgentRule);
        Log.d("TEST_TREO", "createNewChatGemByApi -> loadOrCreateChat xong");

        HSQTools.delay(1000);
    }

    // =========================================================
    // 2. TỰ ĐỘNG CHỤP ẢNH VÀ GỬI LÊN GEMINI
    // =========================================================
    private void loadSurveyBriefMemory()
    {
        try
        {
            android.content.SharedPreferences prefs = HSQConfig.getContext().getSharedPreferences(SURVEY_BRIEF_PREFS, android.content.Context.MODE_PRIVATE);
            surveyBriefMemory = prefs.getString(SURVEY_BRIEF_KEY_TEXT, "");
            surveyBriefFingerprint = prefs.getString(SURVEY_BRIEF_KEY_FP, "");
            if (surveyBriefMemory == null) surveyBriefMemory = "";
            if (surveyBriefFingerprint == null) surveyBriefFingerprint = "";
            if (!surveyBriefMemory.trim().isEmpty())
            {
                Log.d("TEST_TREO", "Loaded survey brief memory | chars=" + surveyBriefMemory.length());
            }
        }
        catch (Exception e)
        {
            surveyBriefMemory = "";
            surveyBriefFingerprint = "";
            Log.e("TEST_TREO", "Load survey brief memory failed | " + e.getMessage());
        }
    }

    private void persistSurveyBriefMemory()
    {
        try
        {
            android.content.SharedPreferences prefs = HSQConfig.getContext().getSharedPreferences(SURVEY_BRIEF_PREFS, android.content.Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(SURVEY_BRIEF_KEY_TEXT, surveyBriefMemory == null ? "" : surveyBriefMemory)
                    .putString(SURVEY_BRIEF_KEY_FP, surveyBriefFingerprint == null ? "" : surveyBriefFingerprint)
                    .apply();
        }
        catch (Exception e)
        {
            Log.e("TEST_TREO", "Persist survey brief memory failed | " + e.getMessage());
        }
    }

    private void resetSurveyBriefMemory(String reason)
    {
        boolean hadMemory = surveyBriefMemory != null && !surveyBriefMemory.trim().isEmpty();
        surveyBriefMemory = "";
        surveyBriefFingerprint = "";
        try
        {
            android.content.SharedPreferences prefs = HSQConfig.getContext().getSharedPreferences(SURVEY_BRIEF_PREFS, android.content.Context.MODE_PRIVATE);
            prefs.edit()
                    .remove(SURVEY_BRIEF_KEY_TEXT)
                    .remove(SURVEY_BRIEF_KEY_FP)
                    .apply();
        }
        catch (Exception e)
        {
            Log.e("TEST_TREO", "Reset survey brief memory failed | reason=" + reason + " | " + e.getMessage());
        }
        if (hadMemory)
        {
            Log.d("TEST_TREO", "Reset survey brief memory | reason=" + reason);
        }
    }

    private void rememberSurveyBriefFromCurrentScreen()
    {
        try
        {
            List<HSQTools.TextBlock> currentScreen = getCheckAnswerSmart();
            String candidate = extractSurveyBriefCandidate(currentScreen);
            if (candidate.isEmpty()) return;

            String normalized = normalizeSurveyBriefForMatch(candidate);
            if (!isSurveyBriefInstruction(normalized)) return;

            rememberSurveyBrief(candidate, normalized);
        }
        catch (Exception e)
        {
            Log.e("TEST_TREO", "Detect survey brief failed | " + e.getMessage());
        }
    }

    private String extractSurveyBriefCandidate(List<HSQTools.TextBlock> screen)
    {
        if (screen == null || screen.isEmpty()) return "";

        List<HSQTools.TextBlock> sorted = new ArrayList<>(screen);
        sorted.sort(Comparator
                .comparingInt((HSQTools.TextBlock block) -> block.y)
                .thenComparingInt(block -> block.x));

        StringBuilder sb = new StringBuilder();
        for (HSQTools.TextBlock block : sorted)
        {
            if (block == null || block.text == null) continue;
            if (block.y <= 180) continue;

            String text = block.text.replaceAll("\\s+", " ").trim();
            if (text.isEmpty() || isSurveyBriefJunkText(text)) continue;

            if (sb.length() > 0) sb.append(' ');
            sb.append(text);

            if (sb.length() > MAX_SURVEY_BRIEF_CHARS * 2) break;
        }

        return clampSurveyBrief(sb.toString());
    }

    private boolean isSurveyBriefJunkText(String text)
    {
        String norm = normalizeSurveyBriefForMatch(text);
        String compact = norm.replace(" ", "");
        return compact.equals("0")
                || compact.equals("selectone")
                || compact.equals("co")
                || compact.equals("khong")
                || compact.equals("tieptuc")
                || compact.equals("continue")
                || compact.equals("back")
                || compact.equals("offerwall")
                || compact.equals("dongy")
                || compact.equals("batdau")
                || compact.equals("start");
    }

    private boolean isSurveyBriefInstruction(String norm)
    {
        if (norm == null || norm.length() < 80) return false;

        int score = 0;
        boolean hasSearchAnchor = containsAny(norm,
                "dang tim kiem", "tim kiem nhung nguoi", "doi tuong khao sat",
                "we are looking for", "we are seeking", "looking for people", "target respondents", "target audience");
        boolean hasCriteriaAnchor = containsAny(norm,
                "tieu chi", "dap ung cac tieu chi", "yeu cau khao sat", "du dieu kien",
                "criteria", "eligibility", "eligible", "qualified", "requirements", "screening criteria");

        if (hasSearchAnchor) score += 3;
        if (hasCriteriaAnchor) score += 3;
        if (containsAny(norm, "dap ung", "phu hop", "qualify", "match")) score += 1;
        if (containsAny(norm, "tuoi", "do tuoi", "age", "aged", "years old")) score += 1;
        if (containsAny(norm, "trong vong", "gan day", "12 thang", "past", "last", "recently")) score += 1;
        if (containsAny(norm, "thanh vien gia dinh", "nguoi quen", "moi ho", "family member", "friend", "someone you know", "invite")) score += 1;
        if (containsAny(norm, "khao sat", "tham gia", "survey", "participate")) score += 1;

        return (hasSearchAnchor || hasCriteriaAnchor) && score >= 5;
    }

    private void rememberSurveyBrief(String candidate, String normalized)
    {
        candidate = clampSurveyBrief(candidate);
        if (candidate.isEmpty()) return;

        if (normalized == null || normalized.isEmpty()) normalized = normalizeSurveyBriefForMatch(candidate);
        String currentNorm = normalizeSurveyBriefForMatch(surveyBriefMemory);
        boolean sameBrief = isSameSurveyBrief(currentNorm, normalized);

        if (sameBrief)
        {
            if (surveyBriefMemory == null || candidate.length() > surveyBriefMemory.length())
            {
                surveyBriefMemory = candidate;
                surveyBriefFingerprint = buildSurveyBriefFingerprint(normalizeSurveyBriefForMatch(surveyBriefMemory));
                persistSurveyBriefMemory();
                Log.d("TEST_TREO", "Updated survey brief memory | chars=" + surveyBriefMemory.length());
            }
            return;
        }

        if (surveyBriefMemory == null || surveyBriefMemory.trim().isEmpty())
        {
            surveyBriefMemory = candidate;
        }
        else
        {
            surveyBriefMemory = clampSurveyBrief(surveyBriefMemory + " " + candidate);
        }

        surveyBriefFingerprint = buildSurveyBriefFingerprint(normalizeSurveyBriefForMatch(surveyBriefMemory));
        persistSurveyBriefMemory();
        Log.d("TEST_TREO", "Remembered survey brief memory | chars=" + surveyBriefMemory.length());
    }

    private boolean isSameSurveyBrief(String currentNorm, String newNorm)
    {
        if (currentNorm == null || currentNorm.isEmpty() || newNorm == null || newNorm.isEmpty()) return false;

        String newProbe = newNorm.substring(0, Math.min(180, newNorm.length()));
        String currentProbe = currentNorm.substring(0, Math.min(180, currentNorm.length()));
        return currentNorm.contains(newProbe) || newNorm.contains(currentProbe);
    }

    private String buildSurveyBriefFingerprint(String normalized)
    {
        if (normalized == null) normalized = "";
        String probe = normalized.substring(0, Math.min(600, normalized.length()));
        return Integer.toHexString(probe.hashCode()) + ":" + probe.length();
    }

    private String appendSurveyBriefToPrompt(String prompt)
    {
        String basePrompt = prompt == null ? "" : prompt;
        String brief = surveyBriefMemory == null ? "" : surveyBriefMemory.trim();
        if (brief.isEmpty() || basePrompt.contains("SURVEY_BRIEF_MEMORY")) return basePrompt;

        return basePrompt
                + "\n\nSURVEY_BRIEF_MEMORY:\n"
                + brief
                + "\nRule: Use this only for the current survey. Keep screening and logic answers consistent with these target/eligibility requirements. Do not explain.";
    }

    private String normalizeSurveyBriefForMatch(String input)
    {
        if (input == null) return "";
        String text = input.replace('Đ', 'D').replace('đ', 'd');
        text = HSQTools.removeAccents(text).toLowerCase(Locale.US);
        return text.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    private String clampSurveyBrief(String text)
    {
        if (text == null) return "";
        String clean = text.replaceAll("\\s+", " ").trim();
        if (clean.length() <= MAX_SURVEY_BRIEF_CHARS) return clean;
        return clean.substring(0, MAX_SURVEY_BRIEF_CHARS - 3).trim() + "...";
    }

    private boolean containsAny(String text, String... needles)
    {
        if (text == null || needles == null) return false;
        for (String needle : needles)
        {
            if (needle != null && !needle.isEmpty() && text.contains(needle)) return true;
        }
        return false;
    }

    private String getAnswerFromGemByApi(int imageCount, boolean splitAnswer, boolean captureScreen, String prompt)
    {
        int tryAgain = 0;
        int apiErrorCount = 0;
        String textAnswer;
        if (prompt.length() < 2)
        {
            prompt = "Bám sát profile và các rule. Không giải thích dài dòng";
        }
        // 1. Chụp ảnh (Nếu cần)
        if (captureScreen)
        {
            updateNotificationContent("Chụp " + imageCount + " hình...");

            // Xóa sạch thư mục cũ và tạo lại mới tinh bằng HSQFileHelper
            HSQFileHelper.deleteFile(imagePath);
            HSQFileHelper.createFolder(imagePath);
            HSQTools.delay(1000);

            // Cắt ảnh bằng fullScreen
            android.graphics.Rect fullScreen = new android.graphics.Rect(0, 0, 9999, 9999);
            HSQTools.cropAndSaveScreen(fullScreen, imagePath + "/screenCapa1.png");
            HSQTools.delay(1000);

            if (imageCount > 1)
            {
                for (int i = 1; i < imageCount; i++)
                {
                    // Chú ý: Ở đây gọi thẳng HSQTools.swipe hoặc ASBLBridgeService.swipe tùy anh
                    swipe(xCenter, yBot, xCenter, yTop, 2000);
                    HSQTools.delay(2000);

                    String fileName = (i + 1 < 10) ? "/screenCapa" + (i + 1) + ".png" : "/screenCapb" + (i - 8) + ".png";
                    HSQTools.cropAndSaveScreen(fullScreen, imagePath + fileName);
                    HSQTools.delay(1000);
                }
                for (int i = 0; i < imageCount; i++)
                {
                    swipe(xCenter, yTop, xCenter, yBot, 2000);
                    HSQTools.delay(2000);
                }
            }
        }
        HSQTools.ScanImage(imagePath);
        HSQTools.delay(2000);
        rememberSurveyBriefFromCurrentScreen();
        // 2. Lấy Base64
        updateNotificationContent("Đang lấy Base64...");
        List<String> listBase64Images = new ArrayList<>();
        File dir = new File(imagePath);

        // Lấy danh sách file ra một mảng tĩnh
        File[] files = dir.listFiles();

        if (files != null)
        {
            // ==========================================================
            // VŨ KHÍ BÍ MẬT: ÉP SẮP XẾP FILE THEO TÊN (Alphabet)
            // Để đảm bảo thứ tự luôn là: Capa1 -> Capa2 -> Capb1 ...
            // ==========================================================
            java.util.Arrays.sort(
                    files, Comparator.comparing(File::getName)
            );

            for (File f : files)
            {
                if (f.getName().endsWith(".png") || f.getName().endsWith(".jpg"))
                {
                    listBase64Images.add(HSQTools.convertImageToBase64(f.getAbsolutePath()));
                    Log.d("TEST_TREO", "Đã lấy ảnh theo đúng thứ tự: " + f.getName());
                }
            }
        }

        updateNotificationContent("Đang chờ API trả lời...");
        // 3. Gửi API
        String promptForHistory = prompt;
        while (true)
        {
            try
            {
                Log.d("TEST_TREO", "1. Chuẩn bị nhảy vào hàm sendMessageWithImages");
                String promptToSend = appendSurveyBriefToPrompt(prompt);
                promptForHistory = prompt;
                textAnswer = AIHelper.sendMessageWithImages(promptToSend, listBase64Images);

                Log.d("TEST_TREO", "4. Đã thoát ra khỏi hàm, kết quả là: " + textAnswer);
                if (textAnswer.startsWith("API Error: 429") || textAnswer.contains("\"code\": 429") || textAnswer.contains("\"status\": \"RESOURCE_EXHAUSTED\"")
                        || textAnswer.contains("Quota exceeded for metric") || textAnswer.contains("\"code\": 403")
                        || textAnswer.contains("has been suspended") || textAnswer.contains("API Error: 530"))
                {
                    delay(15000);
                    if(changedSite == 0 && AIApiKey2 != null && !AIApiKey2.trim().isEmpty())
                    {
                        AIWebSite = AIWebSite2;
                        AIApiKey = AIApiKey2;
                        aiModel = aiModel2;
                        changedSite++;
                    }
                    else {
                        AIWebSite = AIWebSite1;
                        AIApiKey = AIApiKey1;
                        aiModel = aiModel1;
                        changedSite = 0;
                    }
                    try { if (AIHelper != null) AIHelper.freeRam(); } catch (Exception ignored) {}
                    AIHelper = createAIHelper();
                    createNewChatGemByApi(customAgentRule, true);
                    continue;
                }
                else if (textAnswer.startsWith("API Error:") || textAnswer.startsWith("Exception"))
                {
                    if (textAnswer.contains("StackOverflowError"))
                    {
                        updateNotificationContent("StackOverflowError");
                        delay(15000);
                        try
                        {
                            if (AIHelper != null)
                            {
                                AIHelper.freeRam(); // tha object cu som hon, khong can dung lai no nua
                            }
                        }
                        catch (Exception ignored)
                        {
                        }

                        AIHelper = createAIHelper();

                        // true = xoa file history cu neu con ton tai, sau do load lai context system moi tinh
                        createNewChatGemByApi(customAgentRule, true);

                        tryAgain = 0;
                        delay(1500);
                        continue;
                    }
                    apiErrorCount++;
                    if (apiErrorCount == 3) {
                        updateNotificationContent("Lỗi API 502 quá lỳ! Reset não AI...");
                        try { if (AIHelper != null) AIHelper.freeRam(); } catch (Exception ignored) {}
                        AIHelper = createAIHelper();
                        createNewChatGemByApi(customAgentRule, true);
                        delay(2000);
                    }
                    else if (apiErrorCount > 3) {
                        updateNotificationContent("Lỗi API 502 quá lỳ! thay web ...");
                        if(changedSite == 0 && AIApiKey2 != null && !AIApiKey2.trim().isEmpty())
                        {
                            AIWebSite = AIWebSite2;
                            AIApiKey = AIApiKey2;
                            aiModel = aiModel2;
                            changedSite++;
                        }
                        else {
                            AIWebSite = AIWebSite1;
                            AIApiKey = AIApiKey1;
                            aiModel = aiModel1;
                            changedSite = 0;
                        }
                        try { if (AIHelper != null) AIHelper.freeRam(); } catch (Exception ignored) {}
                        AIHelper = createAIHelper();
                        createNewChatGemByApi(customAgentRule, true);
                        apiErrorCount = 0;
                        delay(2000);
                    }
                    else {
                        updateNotificationContent("Lỗi API/Mạng (" + apiErrorCount + "/3): " + textAnswer + ". Đợi 15s...");
                        HSQTools.delay(15000);
                    }
                    continue;
                }
                else if (!textAnswer.contains("|"))
                {
                    if (tryAgain > 0)
                    {
                        createNewChatGemByApi(customAgentRule, true);
                        delay(5000);
                        tryAgain = 0;
                        continue;
                    }
                    tryAgain++;
                    prompt = "câu trả lời chưa đúng format theo rule đặt ra, trả lời lại ngắn gọn theo hướng dẫn";
                    updateNotificationContent("GEM Ngu: " + textAnswer);
                    HSQTools.delay(10000);
                    continue;
                }
                textAnswer = repairDobYearClickToTextAnswer(textAnswer);

                updateNotificationContent("API Tráº£ vá»: " + textAnswer);

                break;
            }
            catch (Exception ex)
            {
                updateNotificationContent("Lỗi kết nối...");
                HSQTools.delay(15000);
            }
        }

        AIHelper.saveTurnToHistory(promptForHistory, listBase64Images, textAnswer);
        if (splitAnswer && textAnswer.contains("|"))
        {
            try
            {
                textAnswer = textAnswer.split("\\|")[1];
            }
            catch (Exception ignored)
            {
            }
        }

        return textAnswer;
    }

    // =========================================================
    // 3. GỬI TEXT CHAY VÀ BẮT LỖI
    // =========================================================
    private String sendChatToGemByApi(String chatContent)
    {
        int tryAgain = 0;
        int apiErrorCount = 0;
        updateNotificationContent("Gửi chat đến API: " + chatContent);

        while (true)
        {
            try
            {
                updateNotificationContent("Đang chờ API trả lời...");
                String chatContentToSend = appendSurveyBriefToPrompt(chatContent);
                String textAnswer = AIHelper.sendMessageWithImages(chatContentToSend, null);
                updateNotificationContent("API Trả về: " + textAnswer);


                if (textAnswer.startsWith("API Error: 429") || textAnswer.contains("\"code\": 429") || textAnswer.contains("\"status\": \"RESOURCE_EXHAUSTED\"")
                        || textAnswer.contains("Quota exceeded for metric") || textAnswer.contains("\"code\": 403")
                        || textAnswer.contains("has been suspended"))
                {
                    delay(15000);
                    if(changedSite == 0 && AIApiKey2 != null && !AIApiKey2.trim().isEmpty())
                    {
                        AIWebSite = AIWebSite2;
                        AIApiKey = AIApiKey2;
                        aiModel = aiModel2;
                        changedSite++;
                    }
                    else {
                        AIWebSite = AIWebSite1;
                        AIApiKey = AIApiKey1;
                        aiModel = aiModel1;
                        changedSite = 0;
                    }
                    try { if (AIHelper != null) AIHelper.freeRam(); } catch (Exception ignored) {}
                    AIHelper = createAIHelper();
                    createNewChatGemByApi(customAgentRule, true);
                    continue;
                }
                else if (textAnswer.startsWith("API Error:") || textAnswer.startsWith("Exception"))
                {
                    if (textAnswer.contains("StackOverflowError"))
                    {
                        updateNotificationContent("StackOverflowError");
                        delay(15000);
                        try
                        {
                            if (AIHelper != null)
                            {
                                AIHelper.freeRam(); // tha object cu som hon, khong can dung lai no nua
                            }
                        }
                        catch (Exception ignored)
                        {
                        }

                        AIHelper = createAIHelper();

                        // true = xoa file history cu neu con ton tai, sau do load lai context system moi tinh
                        createNewChatGemByApi(customAgentRule, true);

                        tryAgain = 0;
                        delay(1500);
                        continue;
                    }
                    apiErrorCount++;
                    if (apiErrorCount == 3) {
                        updateNotificationContent("Lỗi API 502 quá lỳ! Reset não AI...");
                        try { if (AIHelper != null) AIHelper.freeRam(); } catch (Exception ignored) {}
                        AIHelper = createAIHelper();
                        createNewChatGemByApi(customAgentRule, true);
                        delay(2000);
                    }
                    else if (apiErrorCount > 3) {
                        updateNotificationContent("Lỗi API 502 quá lỳ! thay web ...");
                        if(changedSite == 0 && AIApiKey2 != null && !AIApiKey2.trim().isEmpty())
                        {
                            AIWebSite = AIWebSite2;
                            AIApiKey = AIApiKey2;
                            aiModel = aiModel2;
                            changedSite++;
                        }
                        else {
                            AIWebSite = AIWebSite1;
                            AIApiKey = AIApiKey1;
                            aiModel = aiModel1;
                            changedSite = 0;
                        }
                        try { if (AIHelper != null) AIHelper.freeRam(); } catch (Exception ignored) {}
                        AIHelper = createAIHelper();
                        createNewChatGemByApi(customAgentRule, true);
                        apiErrorCount = 0;
                        delay(2000);
                    }
                    else {
                        updateNotificationContent("Lỗi API/Mạng (" + apiErrorCount + "/3): " + textAnswer + ". Đợi 15s...");
                        HSQTools.delay(15000);
                    }
                    continue;
                }
                else if (!textAnswer.contains("|"))
                {
                    if (tryAgain > 0)
                    {
                        createNewChatGemByApi(customAgentRule, true);
                        delay(5000);
                        tryAgain = 0;
                        continue;
                    }
                    tryAgain++;
                    chatContent = "câu trả lời chưa đúng format theo rule đặt ra, trả lời lại ngắn gọn theo hướng dẫn";
                    updateNotificationContent("GEM Ngu: " + textAnswer);
                    HSQTools.delay(10000);
                    continue;
                }
                textAnswer = repairDobYearClickToTextAnswer(textAnswer);

                delay(15000);

                return textAnswer;
            }
            catch (Exception ex)
            {
                updateNotificationContent("Lỗi kết nối...");
                HSQTools.delay(15000);
            }
        }
    }

    private String repairDobYearClickToTextAnswer(String answer)
    {
        if (answer == null || !answer.toLowerCase(Locale.ROOT).contains("clicktotext")) return answer;

        String finalAnswer = extractAutomationFinalAnswer(answer);
        int targetYear = extractLikelyDobYear(finalAnswer);
        if (targetYear < 1900) return answer;

        Matcher stepMatcher = Pattern.compile("clicktotext\\s*\\{\\s*((?:19|20)\\d{2})\\s*\\}").matcher(answer);
        StringBuffer repaired = new StringBuffer();
        boolean changed = false;
        while (stepMatcher.find())
        {
            int stepYear;
            try
            {
                stepYear = Integer.parseInt(stepMatcher.group(1));
            }
            catch (Exception ignored)
            {
                continue;
            }

            if (stepYear != targetYear)
            {
                stepMatcher.appendReplacement(repaired, Matcher.quoteReplacement("clicktotext {" + targetYear + "}"));
                changed = true;
            }
        }
        stepMatcher.appendTail(repaired);

        if (changed)
        {
            String fixed = repaired.toString();
            Log.d("TEST_TREO", "DOB year repair | before=" + answer + " | after=" + fixed);
            return fixed;
        }
        return answer;
    }

    private String extractAutomationFinalAnswer(String answer)
    {
        if (answer == null) return "";
        try
        {
            String[] parts = answer.split("\\|", -1);
            if (parts.length >= 2 && "begin".equalsIgnoreCase(parts[0].trim()))
            {
                return parts[1].trim();
            }
        }
        catch (Exception ignored)
        {
        }
        return "";
    }

    private int extractLikelyDobYear(String raw)
    {
        if (raw == null || raw.trim().isEmpty()) return -1;
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        int maxDobYear = currentYear - 10;

        Matcher dateMatcher = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]((?:19|20)\\d{2})\\b").matcher(raw);
        int best = -1;
        while (dateMatcher.find())
        {
            best = parseDobYear(dateMatcher.group(1), maxDobYear);
        }
        if (best >= 1900) return best;

        Matcher singleYearMatcher = Pattern.compile("^\\s*((?:19|20)\\d{2})\\s*$").matcher(raw);
        if (singleYearMatcher.find())
        {
            return parseDobYear(singleYearMatcher.group(1), maxDobYear);
        }
        return -1;
    }

    private int parseDobYear(String rawYear, int maxDobYear)
    {
        try
        {
            int year = Integer.parseInt(rawYear);
            if (year >= 1900 && year <= maxDobYear) return year;
        }
        catch (Exception ignored)
        {
        }
        return -1;
    }

    private void saveServeyData(RegistrationInfo info, String filePath)
    {
        try
        {
            // 1. Chuyển Object thành chuỗi JSON
            // setPrettyPrinting() tương đương với Formatting.Indented (xuống dòng đẹp mắt)
            // Gson mặc định đã tự động chuyển Enum thành chuỗi String nên không cần EnumConverter
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = gson.toJson(info);

            // 2 & 3. Đảm bảo thư mục tồn tại và ghi đè vào file
            // Dùng luôn cái HSQFileHelper bá đạo của anh, nó tự lo vụ tạo Folder và ép ghi bằng Root
            HSQFileHelper.writeFile(jsonString, filePath);

        }
        catch (Exception ignored)
        {
        }
    }
  
    private void clearAllText()
    {
        inputText("", null, false);
    }
    /**
     * Mắt thần dò ô Input: Tìm ô cho phép nhập liệu nằm gần dòng Text nhất
     */
    private android.graphics.Rect findInputNearYByASBL(android.view.accessibility.AccessibilityNodeInfo root, int labelY)
    {
        if (root == null) return null;

        java.util.List<android.view.accessibility.AccessibilityNodeInfo> allNodes = new java.util.ArrayList<>();
        HSQTools.getAllNodesRec(root, allNodes);

        android.graphics.Rect onlyInput = null;
        android.graphics.Rect bestInput = null;
        int visibleInputCount = 0;
        int bestScore = Integer.MAX_VALUE;

        for (android.view.accessibility.AccessibilityNodeInfo node : allNodes)
        {
            if (node == null) continue;

            String clazz = node.getClassName() != null ? node.getClassName().toString() : "";
            String viewId = node.getViewIdResourceName() != null ? node.getViewIdResourceName().toLowerCase() : "";

            boolean isInput =
                    node.isEditable() ||
                            clazz.contains("EditText") ||
                            clazz.contains("AutoCompleteTextView") ||
                            viewId.contains("answer") ||
                            viewId.contains("input") ||
                            viewId.contains("edit");

            if (!isInput) continue;

            android.graphics.Rect bounds = new android.graphics.Rect();
            node.getBoundsInScreen(bounds);

            if (bounds.width() < 80 || bounds.height() < 20) continue;
            if (bounds.centerY() <= 180 || bounds.centerY() >= heightOfScreen - 80) continue;

            visibleInputCount++;
            onlyInput = new android.graphics.Rect(bounds);

            int centerDist = Math.abs(bounds.centerY() - labelY);
            int topDist = bounds.top - labelY;
            boolean nearEnough =
                    centerDist < 320 ||
                            (topDist >= -160 && topDist <= 1200);

            if (!nearEnough) continue;

            int score = Math.min(centerDist, topDist >= 0 ? topDist : Math.abs(topDist) + 240);
            if (node.isFocusable()) score -= 120;
            if (node.isClickable()) score -= 80;
            if (viewId.contains("answer")) score -= 180;

            if (score < bestScore)
            {
                bestScore = score;
                bestInput = new android.graphics.Rect(bounds);
            }
        }

        if (bestInput != null) return bestInput;
        if (visibleInputCount == 1) return onlyInput;

        return null;
    }

    /**
     * Mắt thần dò Dropdown (Bản nâng cấp tối thượng chống giật ngược):
     * Ép dò đúng hướng (Từ chữ trở xuống), và tự động thưởng điểm cho ô chứa đúng từ khóa đang tìm!
     */
    private android.graphics.Point findDropdownNearByASBL(android.view.accessibility.AccessibilityNodeInfo root, int labelX, int labelY, String targetContext)
    {
        if (root == null) return null;

        String normTarget = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(targetContext == null ? "" : targetContext));
        java.util.List<android.view.accessibility.AccessibilityNodeInfo> allNodes = new java.util.ArrayList<>();
        HSQTools.getAllNodesRec(root, allNodes);

        android.graphics.Point bestPoint = null;
        double minScore = Double.MAX_VALUE;

        for (android.view.accessibility.AccessibilityNodeInfo node : allNodes)
        {
            if (node == null) continue;

            String clazz = node.getClassName() != null ? node.getClassName().toString() : "";
            boolean classLooksInput =
                    clazz.contains("Spinner")
                            || clazz.contains("EditText")
                            || clazz.contains("AutoComplete")
                            || clazz.contains("Button");

            boolean isInteractive = node.isClickable() || node.isFocusable() || classLooksInput;
            if (!isInteractive) continue;

            android.graphics.Rect bounds = new android.graphics.Rect();
            node.getBoundsInScreen(bounds);

            if (bounds.centerY() <= 180 || bounds.centerY() >= heightOfScreen - 80) continue;
            if (bounds.width() < 80 || bounds.height() < 35 || bounds.height() > 320) continue;
            if (bounds.width() > widthOfScreen) continue;

            CharSequence txt = node.getText();
            if (txt == null || txt.toString().trim().isEmpty()) txt = node.getContentDescription();

            String raw = txt == null ? "" : txt.toString().trim();
            String rawCompact = raw.replaceAll("\\s+", "");
            String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(raw));

            boolean isClassicDropdownText =
                    clean.equals("v") || clean.equals("chon") || clean.equals("select") || clean.equals("choose")
                            || clean.equals("chonmot") || clean.equals("selectone") || clean.contains("vuilongchon")
                            || clean.equals("luachon") || clean.equals("vuilongluachon")
                            || clean.equals("haychonmotphuongan") || clean.contains("pleaseselect")
                            || raw.equals("-") || clean.equals("-")
                            || raw.equals("...") || clean.equals("...");

            boolean isMaskedDropdownPlaceholder = rawCompact.matches("^[=._\\-]{3,}$");

            boolean sameTextAsLabel = !normTarget.isEmpty()
                    && !clean.isEmpty()
                    && (
                    clean.equals(normTarget)
                            || (clean.contains(normTarget) && clean.length() <= normTarget.length() + 2)
                            || (normTarget.contains(clean) && clean.length() >= 3)
            );

            boolean nearX =
                    Math.abs(bounds.centerX() - labelX) < 760
                            || Math.abs(bounds.left - labelX) < 760;

            boolean belowLabel =
                    bounds.top >= (labelY - 15)
                            && bounds.top <= (labelY + 950)
                            && nearX;

            boolean inlineRight =
                    Math.abs(bounds.centerY() - labelY) <= 150
                            && bounds.centerX() > labelX + 120;

            if (!belowLabel && !inlineRight) continue;

            if (sameTextAsLabel && !isClassicDropdownText && !isMaskedDropdownPlaceholder && !node.isFocusable() && !classLooksInput)
                continue;

            if (!clean.isEmpty() && clean.length() > 80) continue;

            double score = (Math.abs(bounds.top - labelY) * 1.4) + (Math.abs(bounds.centerX() - labelX) * 0.35);

            if (inlineRight) score -= 140;
            if (bounds.top > labelY + 20) score -= 90;

            if (isMaskedDropdownPlaceholder) score -= 700;
            if (isClassicDropdownText) score -= 450;
            if (node.isFocusable()) score -= 260;
            if (classLooksInput) score -= 170;
            if (node.isClickable()) score -= 90;

            if (sameTextAsLabel) score += 950;
            if (bounds.centerY() <= labelY + 60 && !inlineRight) score += 550;

            try
            {
                String viewId = node.getViewIdResourceName();
                if (viewId != null)
                {
                    String id = viewId.toLowerCase();
                    if (id.endsWith("_c") || id.contains("spinner") || id.contains("dropdown") || id.contains("select"))
                        score -= 180;
                }
            }
            catch (Exception ignored) {}

            if (score < minScore)
            {
                minScore = score;

                int safeX = bounds.left + (int) (bounds.width() * 0.82);
                if (safeX > bounds.right - 20) safeX = bounds.centerX();

                bestPoint = new android.graphics.Point(safeX, bounds.centerY());
            }
        }

        return bestPoint;
    }

    /**
     * Mắt thần gom Checkbox: Lôi cổ toàn bộ Checkbox/Radio trên màn hình cực lẹ
     */
    private java.util.List<android.graphics.Rect> findAllCheckboxesByASBL(android.view.accessibility.AccessibilityNodeInfo root, int minTop)
    {
        java.util.List<android.graphics.Rect> result = new java.util.ArrayList<>();
        if (root == null) return result;

        java.util.List<android.view.accessibility.AccessibilityNodeInfo> allNodes = new java.util.ArrayList<>();
        HSQTools.getAllNodesRec(root, allNodes);

        for (android.view.accessibility.AccessibilityNodeInfo node : allNodes)
        {
            String clazz = node.getClassName() != null ? node.getClassName().toString() : "";
            // Tóm cổ bọn có thuộc tính Checkable hoặc mang class Checkbox/Radio
            if (node.isCheckable() || clazz.contains("CheckBox") || clazz.contains("RadioButton"))
            {
                android.graphics.Rect r = new android.graphics.Rect();
                node.getBoundsInScreen(r);
                if (r.height() > 0 && r.width() > 0 && r.top < 2900 && r.top > minTop)
                {
                    result.add(r);
                }
            }
        }
        return result;
    }

    /**
     * Hàm giải cứu: Biến XML từ uiautomator dump thành danh sách TextBlock
     * để logic ghép cặp Point/Min của sếp chạy bình thường khi ASBL tịt ngóm.
     */
    private List<HSQTools.TextBlock> convertXmlToTextBlocks(String xml)
    {
        List<HSQTools.TextBlock> list = new ArrayList<>();
        if (xml == null || xml.isEmpty()) return list;
        try
        {
            // Quét trọn gói tag node để bóc tách thuộc tính độc lập
            Pattern pattern = Pattern.compile("<node\\s+([^>]*?)bounds=\"\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]\"");
            Matcher matcher = pattern.matcher(xml);
            while (matcher.find())
            {
                String attributes = matcher.group(1);String g2 = matcher.group(2);
                String g3 = matcher.group(3);
                String g4 = matcher.group(4);
                String g5 = matcher.group(5);

// Nếu vì lý do siêu nhiên nào đó mà null thì bỏ qua, đi tìm cụm tiếp theo
                if (g2 == null || g3 == null || g4 == null || g5 == null) continue;

                int left = Integer.parseInt(g2);
                int top = Integer.parseInt(g3);
                int right = Integer.parseInt(g4);
                int bottom = Integer.parseInt(g5);

                int cx = (left + right) / 2;
                int cy = (top + bottom) / 2;

                if (cy > 180 && cy < (heightOfScreen - 150))
                {
                    String text = "";
                    assert attributes != null;
                    Matcher textMatch = Pattern.compile("text=\"([^\"]*)\"").matcher(attributes);
                    if (textMatch.find()) text = textMatch.group(1);

                    String desc = "";
                    Matcher descMatch = Pattern.compile("content-desc=\"([^\"]*)\"").matcher(attributes);
                    if (descMatch.find()) desc = descMatch.group(1);

                    // Ưu tiên text, nếu trống trải thì vồ lấy content-desc
                    String finalTxt = null;
                    if (text != null)
                    {
                        finalTxt = (!text.trim().isEmpty()) ? text : desc;
                    }

                    if (finalTxt != null && !finalTxt.trim().isEmpty())
                    {
                        list.add(new TextBlock(finalTxt, cx, cy));
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return list;
    }

    /**
     * Tìm tọa độ các ô có khả năng là Checkbox/Radio trong file XML Dump
     */
    private java.util.List<android.graphics.Rect> findCheckboxesInXml(String xml, int minTop)
    {
        java.util.List<android.graphics.Rect> list = new java.util.ArrayList<>();
        try
        {
            // Tìm các node có class Check/Radio hoặc có thuộc tính checkable="true"
            Pattern pattern = Pattern.compile("<node.*?class=\"([^\"]*?)\".*?checkable=\"true\".*?bounds=\"\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]\"");
            Matcher matcher = pattern.matcher(xml);
            while (matcher.find())
            {
                int left = Integer.parseInt(Objects.requireNonNull(matcher.group(2)));
                int top = Integer.parseInt(Objects.requireNonNull(matcher.group(3)));
                int right = Integer.parseInt(Objects.requireNonNull(matcher.group(4)));
                int bottom = Integer.parseInt(Objects.requireNonNull(matcher.group(5)));
                if (top > minTop && top < 2900)
                {
                    list.add(new android.graphics.Rect(left, top, right, bottom));
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return list;
    }

    private java.util.List<android.graphics.Rect> findImageCardsInXml(String xml, int minTop)
    {
        java.util.List<android.graphics.Rect> list = new java.util.ArrayList<>();
        try
        {
            // Tìm các thẻ có clickable hoặc focusable để lấy ra các thẻ ảnh cỡ bự
            Pattern pattern = Pattern.compile("<node[^>]*?(?:clickable=\"true\"|focusable=\"true\")[^>]*?bounds=\"\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]\"");
            Matcher matcher = pattern.matcher(xml);
            while (matcher.find())
            {
                int left = Integer.parseInt(matcher.group(1));
                int top = Integer.parseInt(matcher.group(2));
                int right = Integer.parseInt(matcher.group(3));
                int bottom = Integer.parseInt(matcher.group(4));
                int w = right - left;
                int h = bottom - top;
                if (top > minTop && top < 2900 && w >= 150 && h >= 150 && w < widthOfScreen * 0.8 && h < heightOfScreen * 0.5)
                {
                    list.add(new android.graphics.Rect(left, top, right, bottom));
                }
            }
            // Lọc các hình chữ nhật bao trùm lẫn nhau (chỉ lấy node con nhỏ nhất)
            java.util.List<android.graphics.Rect> filtered = new java.util.ArrayList<>();
            for (android.graphics.Rect r1 : list) {
                boolean isParent = false;
                for (android.graphics.Rect r2 : list) {
                    if (r1 != r2 && r1.contains(r2) && (r1.width() > r2.width() || r1.height() > r2.height())) {
                        isParent = true;
                        break;
                    }
                }
                if (!isParent) {
                    filtered.add(r1);
                }
            }
            return filtered;
        }
        catch (Exception ignored)
        {
        }
        return list;
    }

    /**
     * TRẠM KIỂM SOÁT LỖI V2.0: Thông minh, tiết kiệm và lỳ lợm.
     *
     * @param actionType:     Loại hành động (Dropdown, Matrix, ClickToText...)
     * @param stepDetail:     Lệnh gốc Gemini trả về.
     * @param currentVisible: Text hiện tại để check kẹt màn hình.
     * @param aiPrompt:       Câu chửi/hướng dẫn tùy biến gửi cho Gemini.
     */
    private void handleActionFailure(String actionType, String stepDetail, List<HSQTools.TextBlock> currentVisible, String aiPrompt, String mainAnswer)
    {
        updateNotificationContent("Xử lý lỗi " + actionType + "...");

        if (AINguL > 0)
        {
            if (createAgain > 0)
            {
                // ĐẾN ĐÂY MỚI LÀM CÁC TÁC VỤ NẶNG: Dump XML + Chụp ảnh
                String currentXml = HSQTools.getFlexibleXML();
                HSQFileHelper.deleteFile(imagePath);
                HSQFileHelper.createFolder(imagePath);
                delay(1000);
                HSQTools.captureAndSaveScreen(imagePath + "/screenCap.png");
                delay(2000);
                HSQTools.ScanImage(imagePath);

                // CHUYỂN ĐỔI PHONESCREEN SANG MẢNG JSON
                org.json.JSONArray jsonArray = new org.json.JSONArray();
                org.json.JSONObject currentScreen = new org.json.JSONObject();
                org.json.JSONArray currentBlocks = new org.json.JSONArray();
                int screenIndex = 1;
                for (HSQTools.TextBlock tb : phoneScreen) {
                    if (tb.text.equals("Đã vuốt xuống")) {
                        try {
                            currentScreen.put("screen_index", screenIndex++);
                            currentScreen.put("blocks", currentBlocks);
                            jsonArray.put(currentScreen);
                        } catch(Exception ignored){}
                        currentScreen = new org.json.JSONObject();
                        currentBlocks = new org.json.JSONArray();
                    } else {
                        try {
                            org.json.JSONObject b = new org.json.JSONObject();
                            b.put("text", tb.text);
                            b.put("x", tb.x);
                            b.put("y", tb.y);
                            currentBlocks.put(b);
                        } catch(Exception ignored){}
                    }
                }
                String jsonContent = jsonArray.toString();

                // GỬI BÁO CÁO VIP LÊN TELEGRAM
                HSQTools.sendTelegramAlertVIP(deviceID, "LỖI LỲ LỢM [" + actionType.toUpperCase() + "]: " + stepDetail, idTelegram, imagePath + "/screenCap.png", currentXml, jsonContent);
                delay(2000);
                updateNotificationContent("Kẹt " + actionType);
                delay(5000);
                currentVisible = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                while (true)
                {
                    delay(180000);
                    List<HSQTools.TextBlock> act = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                    if (!HSQTools.areAlmostSame(currentVisible, act, 20)) break;
                }
                AINguL = 0;
                createAgain = 0;
                currentState = STATE_GET_ANSWER;
            }
            else
            {
                createAgain++;
                createNewChatGemByApi(customAgentRule, true); // Reset não AI hoàn toàn
                textAnswer = "begin|swipemore|1|end";
                currentState = 3;
            }
        }
        else
        {
            if (!mainAnswer.contains("LocalBrain"))
            {
                AINguL++;
            }

            screenBegin = new ArrayList<>(currentVisible);
            textAnswer = "begin|swipemore|1|end";
            currentState = 3;
        }
    }

    private void click(int x, int y, boolean longPress)
    {
        while (true)
        {
            if (ASBLBridgeService.do_click(x, y, longPress))
            {
                return;
            }
            else
            {
                updateNotificationContent("click trượt " + x + " " + y);
                delay(10000);
            }
        }
    }

    private void inputText(String text, ScreenNode targetObj, boolean delay)
    {
        while (true)
        {
            if (ASBLBridgeService.inputText(text, targetObj, delay))
            {
                return;
            }
            else
            {
                updateNotificationContent("lỗi inputText");
                delay(10000);
            }
        }
    }

    private void swipe(int x1, int y1, int x2, int y2, int delay)
    {
        while (true)
        {
            if (ASBLBridgeService.swipe(x1, y1, x2, y2, delay))
            {
                return;
            }
            else
            {
                updateNotificationContent("lỗi swipe");
                delay(10000);
            }
        }
    }
    private HSQTools.TextBlock mergeLineBlocks(List<HSQTools.TextBlock> lineBlocks)
    {
        if (lineBlocks.size() == 1)
        {
            // Tẩy luôn \n nếu chỉ có 1 khối
            String cleanSingle = lineBlocks.get(0).text.replace("\n", " ").replaceAll("\\s+", " ").trim();
            return new HSQTools.TextBlock(cleanSingle, lineBlocks.get(0).x, lineBlocks.get(0).y);
        }

        // Sort theo X từ Trái sang Phải
        lineBlocks.sort((b1, b2) -> Integer.compare(b1.x, b2.x));

        StringBuilder combinedText = new StringBuilder();
        int sumX = 0, sumY = 0;

        for (int i = 0; i < lineBlocks.size(); i++)
        {
            HSQTools.TextBlock b = lineBlocks.get(i);
            // 🌟 Tẩy sạch \n và nối chữ mượt mà
            combinedText.append(b.text.replace("\n", " ").replaceAll("\\s+", " ").trim());
            if (i < lineBlocks.size() - 1)
            {
                combinedText.append(" ");
            }
            sumX += b.x;
            sumY += b.y;
        }

        int avgX = sumX / lineBlocks.size();
        int avgY = sumY / lineBlocks.size();

        return new HSQTools.TextBlock(combinedText.toString().trim(), avgX, avgY);
    }

    private int snapToRadioX(int finalClickX, int finalClickY) {
        String currentXmlForX = HSQTools.getFlexibleXML();
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXmlForX.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

            List<org.w3c.dom.Element> validRadios = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                if (r != null && Math.abs(r.centerY() - finalClickY) <= 40 && r.centerX() > 0) {
                    String className = node.getAttribute("class").toLowerCase();
                    boolean isExplicitRadio = className.contains("radio") || className.contains("checkbox");
                    boolean isSmallBox = r.width() > 20 && r.width() < 150 && r.height() > 20 && r.height() < 150;
                    boolean isSquareShape = (float) Math.max(r.width(), r.height()) / Math.min(r.width(), r.height()) < 2.0f;
                    boolean hasNoText = node.getAttribute("text").trim().isEmpty() && node.getAttribute("content-desc").trim().isEmpty();

                    if (isExplicitRadio || (isSmallBox && isSquareShape && hasNoText)) {
                        validRadios.add(node);
                    }
                }
            }

            if (!validRadios.isEmpty()) {
                // Loại bỏ trùng lặp: Android thường xếp chồng nhiều View (Checkbox, ImageView, FrameLayout) lên cùng 1 tọa độ
                List<Integer> uniqueCenters = new ArrayList<>();
                for (org.w3c.dom.Element node : validRadios) {
                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                    int cx = r.centerX();
                    boolean exists = false;
                    for (int c : uniqueCenters) {
                        if (Math.abs(c - cx) < 20) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        uniqueCenters.add(cx);
                    }
                }

                if (uniqueCenters.size() == 1) {
                    // 🌟 ĐỘC CÔ CẦU BẠI: Thực chất cả hàng ngang chỉ có đúng 1 lỗ duy nhất (dù bị xếp chồng nhiều node XML)
                    // Cứu cánh cho form dị: Chữ tuốt lề trái, lỗ Checkbox tuốt lề phải (distanceX > 1000px)
                    return uniqueCenters.get(0);
                } else {
                    // Có nhiều lỗ trên cùng 1 hàng ngang (Thường là Matrix Grid) -> Áp dụng luật Khắt khe để tìm thằng gần nhất
                int snappedX = -1;
                int minDistanceX = Integer.MAX_VALUE;
                for (org.w3c.dom.Element node : validRadios) {
                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                    int distanceX = Math.abs(r.centerX() - finalClickX);
                    
                    String className = node.getAttribute("class").toLowerCase();
                    boolean isExplicitRadio = className.contains("radio") || className.contains("checkbox");

                    if (isExplicitRadio) {
                        if (distanceX < 400 && distanceX < minDistanceX) {
                            minDistanceX = distanceX;
                            snappedX = r.centerX();
                        }
                    } else {
                        // isSmallBox
                        if (distanceX < 150 && distanceX < minDistanceX) {
                            minDistanceX = distanceX;
                            snappedX = r.centerX();
                        }
                    }
                }
                if (snappedX > 0) return snappedX;
            }
            }
        } catch (Exception ignored) {
        }
        return finalClickX; // Không tìm thấy hoặc lỗi thì giữ nguyên X cũ
    }

    private int clickToTextMinY = 0;

    private List<TextBlock> clickToText(String textWantToClick)
    {
        while (true)
        {
            List<TextBlock> temp = new ArrayList<>();
            int vuotLenLai = 0, checkLaiScreen = 0;

            timTextLoop:
            while (true)
            {
                // Lấy toàn bộ TextBlock trên màn hình (Đã lọc theo clickToTextMinY để khỏi chọt trùng)
                List<TextBlock> checkAnswer = getCheckAnswerSmart().stream()
                        .filter(x -> x.y > 180 && x.y < 2900 && x.y > clickToTextMinY).collect(Collectors.toList());
                phoneScreen.addAll(checkAnswer);
                phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));
                while (true)
                {
                    // Logic chống kẹt màn hình
                    if (HSQTools.areAlmostSame(temp, checkAnswer, 20))
                    {
                        if (vuotLenLai == 0)
                        {
                            vuotLenLai++;
                            safeSwipeClickToText(ysTop, ysBot);
                            delay(2000);
                            clickToTextMinY = 0; // Màn hình thay đổi -> Reset Y limit!
                        }
                        else
                        {
                            return checkAnswer;
                        }
                    }

                    // Gọi vũ khí tìm kiếm tối thượng từ HSQLibrary (Mức độ tái sử dụng cao nhất, không truyền minY)
                    String currentXmlForCheck = HSQTools.getFlexibleXML();
                    HSQTools.TextBlock target = findStrictClickToTextTarget(textWantToClick, checkAnswer, currentXmlForCheck);
                    if (target == null)
                    {
                        HSQTools.TextBlock fuzzyTarget = HSQTools.findBestTextBlockMatch(textWantToClick, checkAnswer, currentXmlForCheck);
                        if (isTrustworthyClickToTextFuzzyMatch(textWantToClick, fuzzyTarget))
                        {
                            target = fuzzyTarget;
                        }
                        else if (fuzzyTarget != null)
                        {
                            updateNotificationContent("Click Text: Bo fuzzy lech [" + textWantToClick + "] -> [" + fuzzyTarget.text + "]");
                        }
                    }

                    if (target != null) {
                        int finalClickX = target.x;
                        int finalClickY = target.y;

                        android.graphics.Point checkmarkPoint = findChoiceCheckmarkPointForText(textWantToClick, target, checkAnswer);
                        if (checkmarkPoint != null)
                        {
                            finalClickX = checkmarkPoint.x;
                            finalClickY = checkmarkPoint.y;
                            updateNotificationContent("Click Tick-Card: Chot [" + textWantToClick + "] tai " + finalClickX + "," + finalClickY);
                        }
                        else
                        {
                            finalClickX = snapToRadioX(finalClickX, finalClickY);
                            updateNotificationContent("Click Text: Chot [" + textWantToClick + "] tai Y=" + finalClickY);
                        }

                        click(finalClickX, finalClickY, false);
                        
                        // Cập nhật lại Y Limit để click kế tiếp phải nằm DƯỚI nút này
                        clickToTextMinY = finalClickY;
                        
                        break timTextLoop;
                    }

                    // --------------------------------------------------------
                    // 🎯 TẦNG DỰ PHÒNG: LOGIC ĐẶC BIỆT NỮ/FEMALE
                    // --------------------------------------------------------
                    String resultNorm = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(textWantToClick));
                    if (resultNorm.equals("nu") || resultNorm.equals("female"))
                    {
                        boolean isClickByXml = false;
                        try {
                            String currentXmlForNu = HSQTools.getFlexibleXML();
                            javax.xml.parsers.DocumentBuilder builderNu = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
                            org.w3c.dom.Document docNu = builderNu.parse(new java.io.ByteArrayInputStream(currentXmlForNu.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                            org.w3c.dom.NodeList nodesNu = docNu.getElementsByTagName("node");

                            for (int i = 0; i < nodesNu.getLength(); i++) {
                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodesNu.item(i);
                                String xmlText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(node.getAttribute("text") + " " + node.getAttribute("content-desc")));

                                if (xmlText.equals("nu") || xmlText.equals("female")) {
                                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                    if (r != null && r.centerY() > 180 && r.centerY() < heightOfScreen - 50 && r.centerY() > clickToTextMinY) {
                                        int clickX = (r.width() > 400) ? r.left + 80 : r.centerX();
                                        updateNotificationContent("Fallback Nữ: Bắt sống bằng XML tại Y=" + r.centerY());
                                        click(clickX, r.centerY(), false);
                                        clickToTextMinY = r.centerY();
                                        isClickByXml = true;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception ignored) {}

                        if (isClickByXml) break timTextLoop;

                        TextBlock qNode = checkAnswer.stream().filter(x -> HSQTools.normalizeText(x.text).contains("gioitinh") || HSQTools.normalizeText(x.text).contains("gender")).findFirst().orElse(null);
                        TextBlock mNode = checkAnswer.stream().filter(x -> (HSQTools.normalizeText(x.text).equals("nam") || HSQTools.normalizeText(x.text).equals("male")) && (qNode == null || x.y > qNode.y)).findFirst().orElse(null);

                        if (qNode != null && mNode != null)
                        {
                            int deltaY = mNode.y - qNode.y;
                            int estimatedNuY = -1;

                            if (deltaY > 200) {
                                estimatedNuY = mNode.y - 120;
                                updateNotificationContent("Fallback Nữ: Đứng trên chữ Nam tại Y=" + estimatedNuY);
                            } else {
                                int stepY = Math.max(100, Math.min(180, deltaY));
                                estimatedNuY = mNode.y + stepY;
                                updateNotificationContent("Fallback Nữ: Đứng dưới chữ Nam tại Y=" + estimatedNuY);
                            }

                            if (estimatedNuY > clickToTextMinY) {
                                click(mNode.x, estimatedNuY, false);
                                clickToTextMinY = estimatedNuY;
                                break timTextLoop;
                            }
                        }
                    }

                    // --------------------------------------------------------
                    // Fallback Vuốt
                    // --------------------------------------------------------
                    if (checkLaiScreen == 0)
                    {
                        checkAnswer = HSQTools.getOcrTextBlocks().stream().filter(x -> x.y > 180 && x.y < 2750).collect(Collectors.toList());
                        checkLaiScreen++;
                        continue;
                    }
                    checkLaiScreen = 0;
                    temp = checkAnswer;
                    int numericScrollDirection = getNumericClickToTextScrollDirection(textWantToClick, checkAnswer);
                    if (numericScrollDirection > 0)
                    {
                        safeSwipeClickToText(ysBot, ysTop);
                    }
                    else if (numericScrollDirection < 0)
                    {
                        safeSwipeClickToText(ysTop, ysBot);
                    }
                    else if (vuotLenLai == 0)
                    {
                        safeSwipeClickToText(ysBot, ysTop);
                    }
                    else
                    {
                        safeSwipeClickToText(ysTop, ysBot);
                    }
                    delay(2000);
                    clickToTextMinY = 0; // Màn hình thay đổi -> Reset Y limit!
                    break;
                }
            }
            break;
        }
        return null;
    }

    private int getNumericClickToTextScrollDirection(String textWantToClick, List<TextBlock> visibleBlocks)
    {
        String targetClean = HSQTools.getOnlyDigits(textWantToClick);
        if (!targetClean.matches("(19|20)\\d{2}")) return 0;

        int targetYear;
        try
        {
            targetYear = Integer.parseInt(targetClean);
        }
        catch (Exception ignored)
        {
            return 0;
        }

        List<int[]> years = new ArrayList<>();
        if (visibleBlocks != null)
        {
            for (TextBlock block : visibleBlocks)
            {
                if (block == null || block.text == null || block.y <= 180 || block.y >= heightOfScreen - 50) continue;
                Matcher matcher = Pattern.compile("\\b((?:19|20)\\d{2})\\b").matcher(block.text);
                while (matcher.find())
                {
                    try
                    {
                        int year = Integer.parseInt(matcher.group(1));
                        if (year >= 1900 && year <= 2099)
                        {
                            years.add(new int[]{year, block.y});
                        }
                    }
                    catch (Exception ignored)
                    {
                    }
                }
            }
        }

        if (years.size() < 5) return 0;
        years.sort(Comparator.comparingInt(a -> a[1]));

        int inc = 0;
        int dec = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < years.size(); i++)
        {
            int year = years.get(i)[0];
            min = Math.min(min, year);
            max = Math.max(max, year);
            if (i > 0)
            {
                int prev = years.get(i - 1)[0];
                if (year > prev) inc++;
                else if (year < prev) dec++;
            }
        }

        if (targetYear >= min && targetYear <= max) return 0;
        boolean increasingDown = inc >= Math.max(3, dec + 2);
        boolean decreasingDown = dec >= Math.max(3, inc + 2);
        if (!increasingDown && !decreasingDown) return 0;

        if (targetYear < min) return increasingDown ? -1 : 1;
        if (targetYear > max) return increasingDown ? 1 : -1;
        return 0;
    }

    private HSQTools.TextBlock findStrictClickToTextTarget(String textWantToClick, List<TextBlock> checkAnswer, String currentXmlForCheck)
    {
        String targetNorm = cleanClickToTextMatchText(textWantToClick);
        if (targetNorm.isEmpty()) return null;

        HSQTools.TextBlock fromScreen = findExactClickToTextInBlocks(textWantToClick, checkAnswer);
        if (fromScreen != null) return fromScreen;

        HSQTools.TextBlock fromXml = findExactClickToTextInXml(textWantToClick, currentXmlForCheck);
        if (fromXml != null) return fromXml;

        return findExactClickToTextInBlocks(textWantToClick, HSQTools.getOcrTextBlocks());
    }

    private HSQTools.TextBlock findExactClickToTextInBlocks(String textWantToClick, List<? extends HSQTools.TextBlock> blocks)
    {
        if (blocks == null || blocks.isEmpty()) return null;
        String targetNorm = cleanClickToTextMatchText(textWantToClick);
        if (targetNorm.isEmpty()) return null;

        HSQTools.TextBlock best = null;
        for (HSQTools.TextBlock block : blocks)
        {
            if (block == null || block.text == null) continue;
            if (block.y <= 180 || block.y >= heightOfScreen - 50 || block.y <= clickToTextMinY) continue;
            if (!isExactClickToTextText(block.text, targetNorm)) continue;

            if (best == null || block.y > best.y)
            {
                best = block;
            }
        }
        return best;
    }

    private HSQTools.TextBlock findExactClickToTextInXml(String textWantToClick, String xml)
    {
        if (xml == null || xml.trim().isEmpty()) return null;
        String targetNorm = cleanClickToTextMatchText(textWantToClick);
        if (targetNorm.isEmpty()) return null;

        try
        {
            javax.xml.parsers.DocumentBuilder builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

            HSQTools.TextBlock best = null;
            for (int i = 0; i < nodes.getLength(); i++)
            {
                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                String rawText = ((node.getAttribute("text") == null ? "" : node.getAttribute("text")) + " " +
                        (node.getAttribute("content-desc") == null ? "" : node.getAttribute("content-desc"))).trim();
                if (!isExactClickToTextText(rawText, targetNorm)) continue;

                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                if (r == null || r.width() <= 0 || r.height() <= 0) continue;
                if (r.centerY() <= 180 || r.centerY() >= heightOfScreen - 50 || r.centerY() <= clickToTextMinY) continue;

                HSQTools.TextBlock candidate = new HSQTools.TextBlock(rawText, r.centerX(), r.centerY());
                if (best == null || candidate.y > best.y)
                {
                    best = candidate;
                }
            }
            return best;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private boolean isTrustworthyClickToTextFuzzyMatch(String textWantToClick, HSQTools.TextBlock target)
    {
        if (target == null || target.text == null) return false;

        String targetNorm = cleanClickToTextMatchText(textWantToClick);
        String nodeNorm = cleanClickToTextMatchText(target.text);
        if (targetNorm.isEmpty() || nodeNorm.isEmpty()) return false;
        if (isExactClickToTextText(target.text, targetNorm)) return true;

        String targetCompact = targetNorm.replace("-", "");
        String nodeCompact = nodeNorm.replace("-", "");
        if (targetCompact.length() < 3 || nodeCompact.length() < 3) return false;

        if (HSQTools.containsOcrFriendly(nodeCompact, targetCompact))
        {
            return nodeCompact.length() <= targetCompact.length() + 12;
        }

        if (HSQTools.containsOcrFriendly(targetCompact, nodeCompact))
        {
            return nodeCompact.length() >= Math.max(5, (int) (targetCompact.length() * 0.75f));
        }

        int allowed = Math.max(1, (int) (targetCompact.length() * 0.18f));
        return Math.abs(nodeCompact.length() - targetCompact.length()) <= allowed + 1
                && HSQTools.levenshtein(nodeCompact, targetCompact) <= allowed;
    }

    private boolean isExactClickToTextText(String rawText, String targetNorm)
    {
        String nodeNorm = cleanClickToTextMatchText(rawText);
        if (nodeNorm.isEmpty() || targetNorm == null || targetNorm.isEmpty()) return false;
        if (nodeNorm.equals(targetNorm) || HSQTools.equalsOcrFriendly(nodeNorm, targetNorm)) return true;
        if (isNumericClickToTextEquivalent(nodeNorm, targetNorm)) return true;

        String nodeCompact = nodeNorm.replace("-", "");
        String targetCompact = targetNorm.replace("-", "");
        if (isNumericClickToTextEquivalent(nodeCompact, targetCompact)) return true;
        return nodeCompact.equals(targetCompact) || HSQTools.equalsOcrFriendly(nodeCompact, targetCompact);
    }

    private boolean isNumericClickToTextEquivalent(String nodeText, String targetText)
    {
        if (nodeText == null || targetText == null) return false;
        if (!nodeText.matches("\\d+") || !targetText.matches("\\d+")) return false;
        try
        {
            return Integer.parseInt(nodeText) == Integer.parseInt(targetText);
        }
        catch (Exception ignored)
        {
            return nodeText.replaceFirst("^0+(?!$)", "").equals(targetText.replaceFirst("^0+(?!$)", ""));
        }
    }

    private String cleanClickToTextMatchText(String rawText)
    {
        if (rawText == null) return "";
        return HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawText));
    }

    private void safeSwipeClickToText(int fromY, int toY)
    {
        smartScroll(fromY, toY, "clicktotext");
    }

    private boolean smartScroll(int fromY, int toY, String reason)
    {
        int safeFromY = Math.max(220, Math.min(heightOfScreen - 180, fromY));
        int safeToY = Math.max(220, Math.min(heightOfScreen - 180, toY));
        boolean forward = safeFromY > safeToY;

        String beforeSignature = getSmartScrollXmlSignature();
        if (tryAccessibilitySmartScroll(forward, beforeSignature, reason))
        {
            return true;
        }

        int safeX = findSafeSwipeXForClickToText(safeFromY, safeToY);
        safeFromY = findSafeSwipeYForClickToText(safeX, safeFromY);
        updateNotificationContent("SmartScroll touch(" + reason + "): " + safeX + "," + safeFromY + " -> " + safeToY);
        swipe(safeX, safeFromY, safeX, safeToY, swipeDuration);
        return true;
    }

    private boolean scrollDropdownForward()
    {
        if (tryDropdownAccessibilityScroll(true))
        {
            updateNotificationContent("DropdownScroll ASBL");
            return true;
        }

        forceDropdownSwipeForward();
        return true;
    }

    private boolean tryDropdownAccessibilityScroll(boolean forward)
    {
        android.graphics.Rect bounds = getActiveDropdownSwipeBounds();
        try
        {
            return ASBLBridgeService.scrollScrollableInBounds(
                    forward,
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom
            );
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    private void forceDropdownSwipeForward()
    {
        android.graphics.Rect bounds = getActiveDropdownSwipeBounds();
        int safeX = Math.max(48, Math.min(widthOfScreen - 48, bounds.centerX()));
        int safeFromY = Math.max(bounds.top + 140, Math.min(bounds.bottom - 90, ysBot));
        int safeToY = Math.max(bounds.top + 90, Math.min(bounds.bottom - 220, ysTop));

        if (safeFromY - safeToY < 420)
        {
            safeFromY = Math.min(heightOfScreen - 180, bounds.bottom - 90);
            safeToY = Math.max(220, bounds.top + 90);
        }

        int duration = Math.max(260, Math.min(650, Math.abs(safeFromY - safeToY) / 3));
        updateNotificationContent("DropdownScroll force: " + safeX + "," + safeFromY + " -> " + safeToY);
        if (!ASBLBridgeService.swipeStraight(safeX, safeFromY, safeX, safeToY, duration))
        {
            swipe(safeX, safeFromY, safeX, safeToY, duration);
        }
    }

    private android.graphics.Rect getActiveDropdownSwipeBounds()
    {
        if (activeDropdownSwipeBounds != null && activeDropdownSwipeBounds.width() > 120 && activeDropdownSwipeBounds.height() > 180)
        {
            return new android.graphics.Rect(activeDropdownSwipeBounds);
        }

        int top = Math.max(180, ysTop - 80);
        int bottom = Math.min(heightOfScreen - 120, ysBot + 80);
        if (bottom - top < 360)
        {
            top = Math.max(180, yTop);
            bottom = Math.min(heightOfScreen - 120, yBot);
        }
        return new android.graphics.Rect(24, top, widthOfScreen - 24, bottom);
    }

    private boolean tryAccessibilitySmartScroll(boolean forward, String beforeSignature, String reason)
    {
        try
        {
            boolean ok = forward ? ASBLBridgeService.scrollForward() : ASBLBridgeService.scrollBackward();
            if (!ok) return false;

            delay(850);
            String afterSignature = getSmartScrollXmlSignature();
            if (!beforeSignature.isEmpty() && !afterSignature.isEmpty() && !beforeSignature.equals(afterSignature))
            {
                updateNotificationContent("SmartScroll ASBL(" + reason + "): " + (forward ? "down" : "up"));
                return true;
            }
        }
        catch (Exception ignored) {}

        return false;
    }

    private String getSmartScrollXmlSignature()
    {
        try
        {
            String xml = HSQTools.getFlexibleXML();
            if (xml == null || xml.trim().isEmpty()) return "";
            return xml.replaceAll("focused=\"(?:true|false)\"", "")
                    .replaceAll("selected=\"(?:true|false)\"", "")
                    .replaceAll("\\s+", " ")
                    .trim();
        }
        catch (Exception ignored)
        {
            return "";
        }
    }

    private int findSafeSwipeXForClickToText(int fromY, int toY)
    {
        int[] candidates = new int[]{
                widthOfScreen - 72,
                72,
                widthOfScreen - 144,
                144,
                widthOfScreen - 220,
                220,
                xs
        };

        String xml = "";
        try { xml = HSQTools.getFlexibleXML(); } catch (Exception ignored) {}

        int bestX = xs;
        int bestScore = Integer.MAX_VALUE;
        for (int rawX : candidates)
        {
            int x = Math.max(24, Math.min(widthOfScreen - 24, rawX));
            int score = scoreSwipeColumnRisk(xml, x, fromY, toY);
            score += Math.abs(x - (widthOfScreen - 72)) / 8; // uu tien gutter phai neu rui ro ngang nhau
            if (score < bestScore)
            {
                bestScore = score;
                bestX = x;
            }
        }

        return bestX;
    }

    private int findSafeSwipeYForClickToText(int x, int preferredY)
    {
        String xml = "";
        try { xml = HSQTools.getFlexibleXML(); } catch (Exception ignored) {}
        int[] offsets = new int[]{0, -140, 140, -260, 260, -380, 380};
        for (int offset : offsets)
        {
            int y = Math.max(220, Math.min(heightOfScreen - 180, preferredY + offset));
            if (!isSwipePointRisky(xml, x, y)) return y;
        }
        return Math.max(220, Math.min(heightOfScreen - 180, preferredY));
    }

    private int scoreSwipeColumnRisk(String xml, int x, int fromY, int toY)
    {
        int score = 0;
        if (isSwipePointRisky(xml, x, fromY)) score += 10000;
        if (isSwipePointRisky(xml, x, toY)) score += 3500;

        try
        {
            if (xml == null || xml.trim().isEmpty()) return score;
            javax.xml.parsers.DocumentBuilder builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");
            int top = Math.min(fromY, toY);
            int bottom = Math.max(fromY, toY);

            for (int i = 0; i < nodes.getLength(); i++)
            {
                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                if (r == null || r.width() <= 0 || r.height() <= 0) continue;
                if (r.bottom < top || r.top > bottom) continue;
                if (x < r.left - 10 || x > r.right + 10) continue;
                if (r.top < 180 || r.bottom > heightOfScreen - 20) continue;

                String text = node.getAttribute("text");
                String desc = node.getAttribute("content-desc");
                String clazz = node.getAttribute("class");
                if (isIgnorableSwipeRiskNode(r, clazz)) continue;
                boolean hasText = (text != null && !text.trim().isEmpty()) || (desc != null && !desc.trim().isEmpty());
                boolean clickable = "true".equals(node.getAttribute("clickable"));
                boolean riskyClass = clazz != null && (clazz.contains("TextView") || clazz.contains("Image") || clazz.contains("Button") || clazz.contains("View"));

                if (clickable) score += 900;
                if (hasText) score += 650;
                if (riskyClass && r.height() > 60) score += 180;
                if (clickable && hasText) score += 1200;
            }
        }
        catch (Exception ignored) {}

        return score;
    }

    private boolean isSwipePointRisky(String xml, int x, int y)
    {
        try
        {
            if (xml == null || xml.trim().isEmpty()) return false;
            javax.xml.parsers.DocumentBuilder builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

            for (int i = 0; i < nodes.getLength(); i++)
            {
                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                if (r == null || r.width() <= 0 || r.height() <= 0) continue;
                if (x < r.left || x > r.right || y < r.top || y > r.bottom) continue;
                if (r.top < 180 || r.bottom > heightOfScreen - 20) continue;

                String text = node.getAttribute("text");
                String desc = node.getAttribute("content-desc");
                String clazz = node.getAttribute("class");
                if (isIgnorableSwipeRiskNode(r, clazz)) continue;
                boolean hasText = (text != null && !text.trim().isEmpty()) || (desc != null && !desc.trim().isEmpty());
                boolean clickable = "true".equals(node.getAttribute("clickable"));
                boolean riskyClass = clazz != null && (clazz.contains("TextView") || clazz.contains("Image") || clazz.contains("Button"));
                if (clickable || hasText || riskyClass) return true;
            }
        }
        catch (Exception ignored) {}
        return false;
    }
    private boolean isIgnorableSwipeRiskNode(android.graphics.Rect r, String clazz)
    {
        if (r == null) return true;
        String className = clazz == null ? "" : clazz;
        boolean hugeContainer = r.width() >= widthOfScreen * 0.85f && r.height() >= heightOfScreen * 0.45f;
        if (hugeContainer) return true;
        if ((className.contains("WebView") || className.contains("FrameLayout") || className.contains("LinearLayout"))
                && r.width() >= widthOfScreen * 0.70f && r.height() >= heightOfScreen * 0.30f)
            return true;
        return false;
    }
    private android.graphics.Point findChoiceCheckmarkPointForText(String textWantToClick, HSQTools.TextBlock target, List<HSQTools.TextBlock> visibleBlocks)
    {
        if (target == null || !isCheckmarkChoiceScreen(visibleBlocks)) return null;

        android.graphics.Point greenPoint = findGreenCheckmarkPointNearTarget(target, visibleBlocks);
        if (greenPoint != null)
        {
            return greenPoint;
        }

        return null;
    }

    private boolean isCheckmarkChoiceScreen(List<HSQTools.TextBlock> visibleBlocks)
    {
        if (visibleBlocks == null || visibleBlocks.isEmpty()) return false;

        for (HSQTools.TextBlock block : visibleBlocks)
        {
            if (block == null || block.text == null) continue;
            String raw = block.text.trim();
            if (raw.isEmpty()) continue;

            String rawLower = raw.toLowerCase(Locale.US);
            String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(raw));
            boolean hasTick = raw.contains("\u2713") || raw.contains("\u2714") || raw.contains("\u2705")
                    || clean.contains("tich") || clean.contains("check") || clean.contains("tick");
            boolean hasTap = clean.contains("nhap") || clean.contains("bam") || clean.contains("tap") || clean.contains("click");
            boolean hasChoose = clean.contains("luachon") || clean.contains("chon") || clean.contains("select")
                    || clean.contains("choose") || clean.contains("thichnhat") || rawLower.contains("like best");

            if ((hasTick && (hasTap || hasChoose)) || (hasTap && hasChoose)) return true;
        }

        return false;
    }

    private android.graphics.Point findGreenCheckmarkPointNearTarget(HSQTools.TextBlock target, List<HSQTools.TextBlock> visibleBlocks)
    {
        android.graphics.Bitmap bmp = null;
        try
        {
            bmp = HSQTools.getScreenBitmap();
            if (bmp == null) return null;

            int screenW = bmp.getWidth();
            int screenH = bmp.getHeight();
            if (screenW <= 0 || screenH <= 0) return null;

            int nextTextY = screenH - 120;
            if (visibleBlocks != null)
            {
                for (HSQTools.TextBlock block : visibleBlocks)
                {
                    if (block == null || block.text == null) continue;
                    if (block.y <= target.y + 180 || block.y >= nextTextY) continue;

                    String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(block.text));
                    if (clean.isEmpty()) continue;
                    if (clean.contains("nhap") && (clean.contains("luachon") || clean.contains("chon"))) continue;
                    nextTextY = block.y;
                }
            }

            int searchTop = Math.max(181, target.y - 180);
            int searchBottom = Math.min(screenH - 120, target.y + 820);
            if (nextTextY < searchBottom && nextTextY - target.y > 250)
            {
                searchBottom = Math.max(target.y + 180, nextTextY - 20);
            }

            int searchLeft = Math.max((int) (screenW * 0.50f), Math.min(target.x + 80, (int) (screenW * 0.75f)));
            int searchRight = screenW - 12;
            if (searchBottom <= searchTop || searchRight <= searchLeft) return null;

            long sumX = 0L;
            long sumY = 0L;
            int count = 0;
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int step = 4;

            for (int y = searchTop; y < searchBottom; y += step)
            {
                for (int x = searchLeft; x < searchRight; x += step)
                {
                    int color = bmp.getPixel(x, y);
                    if (!isLikelyGreenCheckPixel(color)) continue;

                    sumX += x;
                    sumY += y;
                    count++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }

            if (count < 40) return null;

            int blobW = maxX - minX + step;
            int blobH = maxY - minY + step;
            if (blobW < 45 || blobH < 45) return null;
            if (blobW > screenW * 0.38f || blobH > 460) return null;

            float ratio = (float) Math.max(blobW, blobH) / Math.max(1, Math.min(blobW, blobH));
            if (ratio > 2.4f) return null;

            int clickX = (int) (sumX / count);
            int clickY = (int) (sumY / count);
            if (clickX < searchLeft || clickX > searchRight || clickY < searchTop || clickY > searchBottom) return null;

            return new android.graphics.Point(clickX, clickY);
        }
        catch (Exception ignored)
        {
            return null;
        }
        finally
        {
            try
            {
                if (bmp != null && !bmp.isRecycled()) bmp.recycle();
            }
            catch (Exception ignored) {}
        }
    }

    private boolean isLikelyGreenCheckPixel(int color)
    {
        int r = android.graphics.Color.red(color);
        int g = android.graphics.Color.green(color);
        int b = android.graphics.Color.blue(color);
        return g >= 90 && r <= 80 && b <= 140 && g >= r + 45 && g >= b + 15;
    }
    private boolean shouldBlockClickButtonOnUnfinishedPicker(List<TextBlock> visibleBlocks)
    {
        try
        {
            int yearOptionCount = 0;
            boolean hasDobPlaceholder = false;

            if (visibleBlocks != null)
            {
                for (TextBlock block : visibleBlocks)
                {
                    if (block == null || block.text == null) continue;
                    String raw = block.text.trim();
                    if (raw.contains("--MM--") || raw.contains("--DD--") || raw.contains("--YY--") || raw.contains("--YYYY--"))
                    {
                        hasDobPlaceholder = true;
                    }

                    if (raw.matches("^(19|20)\\d{2}$"))
                    {
                        yearOptionCount++;
                    }
                }
            }

            if (hasDobPlaceholder || yearOptionCount >= 5) return true;

            String xml = HSQTools.getFlexibleXML();
            if (xml == null || xml.isEmpty()) return false;
            if (xml.contains("--MM--") || xml.contains("--DD--") || xml.contains("--YY--") || xml.contains("--YYYY--")) return true;

            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

            int radioLikeCount = 0;
            int xmlYearCount = 0;
            for (int i = 0; i < nodes.getLength(); i++)
            {
                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                if (r == null || r.centerY() <= 180 || r.centerY() >= heightOfScreen - 50) continue;

                String clazz = node.getAttribute("class");
                String rawText = (node.getAttribute("text") + " " + node.getAttribute("content-desc")).trim();

                if (clazz.contains("RadioButton") || "true".equals(node.getAttribute("checkable")))
                {
                    radioLikeCount++;
                }
                if (rawText.matches(".*\\b(19|20)\\d{2}\\b.*"))
                {
                    xmlYearCount++;
                }
            }

            return radioLikeCount >= 5 && xmlYearCount >= 5;
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    private List<TextBlock> clickButton(String step)
    {
        int slVuot = 0;
        int navRevealSwipeCount = 0;
        String currentXml = "";

        Matcher matchBtn = java.util.regex.Pattern.compile("\\{([^{}]+)\\}").matcher(step);
        String rawTarget = matchBtn.find() ? matchBtn.group(1).trim() : "";

        boolean isArrow = rawTarget.contains(">") || rawTarget.contains("->") || rawTarget.contains("â†’");
        final String cleanTargetNorm = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawTarget));
        final String targetNorm = isArrow ? rawTarget : cleanTargetNorm;
        final boolean isCardNextTarget = cleanTargetNorm.matches("^(nextthe|nextcard|cardnext|chuyenthe|chuyenthetheo|thetieptheo|tiepthenay|thesau|muitenthe|arrowcard|nextattribute|attribute_next)$");
        final boolean isPageNextTarget = cleanTargetNorm.matches("^(nextpage|pagenext|chuyentrang|trangtieptheo|tieptheotrang|submitpage|submitform|hoanthanhtrang)$");
        final boolean isAgreeOnlyTarget = cleanTargetNorm.matches("^(agree|accept|dongy|toidongy|iagree|chapnhan)$");

        // Phan biet next the va next page. Dau > tu AI chi la tin hieu mo ho, code van phai doc context man hinh.
        final boolean isNextIntent = isArrow || isCardNextTarget || isPageNextTarget || cleanTargetNorm.matches("^(continue|next|submit|tieptuc|tieptheo|tiepthe|trangtieptheo|tieptheo>|done|gui|send|agree|accept|agreeandcontinue|>|>>|>>>|gotonextquestion|fwd|forward|tiep|batdau|muiten|arrow|tien|tienlen)$");
        final boolean shouldPreferPageNavigationXml = isNextIntent && !isCardNextTarget && !isAgreeOnlyTarget;
        checkButtonAgainLoop:
        while (true)
        {
            List<HSQTools.TextBlock> tempCompare = new ArrayList<>();
            int vuotLenLai = 0;

            while (true)
            {
                // TẦNG 1: HÌNH ẢNH
                List<HSQTools.TextBlock> smartList = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                phoneScreen.addAll(smartList.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
                phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));
                if (isNextIntent && shouldBlockClickButtonOnUnfinishedPicker(smartList))
                {
                    updateNotificationContent("Chan clickbutton: dropdown/DOB chua hoan tat");
                    List<TextBlock> blockedScreen = smartList.stream().filter(x -> x.y > 180).collect(Collectors.toList());
                    blockedScreen.add(new TextBlock("BLOCKED_CLICKBUTTON_DROPDOWN_OR_DOB_PLACEHOLDER", 0, 0));
                    return blockedScreen;
                }

                if (HSQTools.getImageExistss(
                        2, true, R.drawable.btr_nextser_niq, R.drawable.btr_nextser_es, R.drawable.btr_nextser,
                        R.drawable.btr_next_ifm) != 0)
                {
                    break checkButtonAgainLoop;
                }


                // 🌟 TẦNG 2.1: SĂN NÚT ẢNH TRỐNG (ĐÃ BỌC THÉP Ý NIỆM NEXT)
                // Kể cả AI hô "tieptuc", vẫn cho phép quét tìm nút mũi tên trống ở nửa dưới màn hình!
                if (isNextIntent)
                {
                    try
                    {
                        currentXml = HSQTools.getFlexibleXML();
                        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                        org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                        org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");
                        int bottomOverlayTop = findLikelyBottomOverlayTop(nodes);

                        android.graphics.Rect bestBlankBtn = null;
                        int maxBlankY = 0;

                        for (int i = 0; i < nodes.getLength(); i++)
                        {
                            org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                            String clazz = node.getAttribute("class");
                            String text = node.getAttribute("text");
                            String desc = node.getAttribute("content-desc");
                            String clickable = node.getAttribute("clickable"); // 🔥 THUỐC ĐỘC DIỆT GHOST NODE

                            // 1. Phải là dạng Button/Image trống không có chữ (Tuyệt đối loại trừ RadioButton, CheckBox)
                            if ((clazz.contains("Button") || clazz.contains("ImageView") || clazz.contains("Image"))
                                    && !clazz.contains("RadioButton")
                                    && !clazz.contains("CheckBox")
                                    && !clazz.contains("ToggleButton")
                                    && !clazz.contains("CompoundButton")
                                    && text.trim().isEmpty() && desc.trim().isEmpty())
                            {
                                // 2. 🔥 CHỐT CHẶN 1: Bắt buộc phải bấm được (clickable="true")
                                if ("true".equals(clickable))
                                {
                                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                    if (r != null)
                                    {
                                        int btnWidth = r.width();
                                        int btnHeight = r.height();
                                        if (isLikelyFloatingScrollTopControl(r, text + " " + desc + " " + node.getAttribute("resource-id") + " " + clazz, smartList))
                                        {
                                            continue;
                                        }


                                        // 3. 🔥 CHỐT CHẶN 2: Kiểm tra kích thước (Tránh tracking pixel 1x1 và tránh ảnh nền bự chà bá)
                                        if (btnWidth > 40 && btnHeight > 40 && btnHeight < 400 && btnWidth < (heightOfScreen * 0.8))
                                        {
                                            // 4. 🔥 CHỐT CHẶN 3: Nằm ở 35% dưới đáy màn hình
                                            if (r.centerY() > (heightOfScreen * 0.65) && r.centerY() < (heightOfScreen - 50))
                                            {
                                                // 5. Luôn ưu tiên lưu thằng nằm thấp hơn dưới đáy
                                                // NẾU cùng độ cao (lệch <= 20px) thì ưu tiên thằng BÊN PHẢI (Next luôn nằm bên phải Back)
                                                if (r.centerY() > maxBlankY + 20)
                                                {
                                                    maxBlankY = r.centerY();
                                                    bestBlankBtn = r;
                                                }
                                                else if (Math.abs(r.centerY() - maxBlankY) <= 20)
                                                {
                                                    if (bestBlankBtn == null || r.centerX() > bestBlankBtn.centerX())
                                                    {
                                                        maxBlankY = r.centerY();
                                                        bestBlankBtn = r;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Quét xong cả bảng XML mới chốt hạ thằng ngon nhất dưới đáy!
                        if (bestBlankBtn != null)
                        {
                            updateNotificationContent("Đồng hóa Next: Bắt sống NÚT ẢNH TRỐNG XỊN tại " + bestBlankBtn.centerX() + "," + bestBlankBtn.centerY());
                            int clickY = getVisibleClickYAvoidingBottomOverlay(bestBlankBtn, bottomOverlayTop);
                            smartList = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                            click(bestBlankBtn.centerX(), clickY, false);
                            if (!checkNextOK(smartList, step))
                            {
                                swipeToTop(slVuot, false);
                            }
                            break checkButtonAgainLoop; // Quay xe thoát hiểm thành công
                        }
                    }
                    catch (Exception ignored)
                    {
                    }
                }

                // 🌟 TẦNG 2.2: LƯỚI QUÉT TIÊU CHUẨN (MẮT THẦN OCR)
                if (shouldPreferPageNavigationXml)
                {
                    int navAction = tryClickPageNavigationFromXml(step, smartList, slVuot, navRevealSwipeCount < 3);
                    if (navAction == 1)
                    {
                        break checkButtonAgainLoop;
                    }
                    if (navAction == 2)
                    {
                        navRevealSwipeCount++;
                        delay(1800);
                        continue;
                    }
                }

                if (isNextIntent && !isPageNextTarget && tryClickWebSurveyVisualArrowFallback(step, smartList, slVuot))
                {
                    break checkButtonAgainLoop;
                }
                HSQTools.TextBlock btnSmart = null;
                int bestSmartScore = Integer.MIN_VALUE;
                for (HSQTools.TextBlock candidate : smartList)
                {
                    int score = getClickButtonOcrMatchScore(candidate, targetNorm, isArrow, isNextIntent, isAgreeOnlyTarget);
                    if (score > bestSmartScore)
                    {
                        bestSmartScore = score;
                        btnSmart = candidate;
                    }
                }

                if (btnSmart != null)
                {
                    int finalX = btnSmart.x;
                    // BỌC THÉP OCR GỘP CHỮ: Nếu nó gộp "BACK NEXT" thì X center sẽ bị lệch sang trái (rơi vào nút BACK).
                    // Chữa cháy: Dời tọa độ X sang bên phải (75% màn hình) để chắc chắn ăn vào nút NEXT.
                    if (isNextIntent) {
                        String cleanTest = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(btnSmart.text));
                        if (cleanTest.contains("back") && btnSmart.x < widthOfScreen / 2) {
                            finalX = (int) (widthOfScreen * 0.75);
                        }
                    }

                    updateNotificationContent("Đồng hóa Next: OCR chốt nút [" + btnSmart.text + "] tại Y=" + btnSmart.y);
                    smartList = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                    click(finalX, btnSmart.y, false);
                    if (!checkNextOK(smartList, step))
                    {
                        swipeToTop(slVuot, false);
                    }
                    break checkButtonAgainLoop;
                }

                // 🌟 TẦNG 3: TRUY QUÉT BẰNG XML (ĐÃ BỌC THÉP CHO IMAGE CARD TRONG WEBVIEW)
                currentXml = HSQTools.getFlexibleXML();
                try
                {
                    javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                    javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                    org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");
                    int bottomOverlayTop = findLikelyBottomOverlayTop(nodes);

                    android.graphics.Rect bestXmlBtnRect = null;
                    int maxCenterY = 0;
                    int bestXmlCustomScore = Integer.MIN_VALUE;

                    for (int i = 0; i < nodes.getLength(); i++)
                    {
                        org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                        String text = node.getAttribute("text");
                        String desc = node.getAttribute("content-desc");
                        String resId = node.getAttribute("resource-id");

                        String rawFullText = text + " " + desc;
                        String cleanFullText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawFullText));

                        boolean isMatch = false;
                        int xmlCustomScore = Integer.MIN_VALUE;

                        // 1. Áp dụng đồng hóa cho XML
                        if (isNextIntent)
                        {
                            android.graphics.Rect rCheck = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                            boolean isInvalidTopText = rCheck != null && rCheck.centerY() < heightOfScreen * 0.2;
                            boolean isQuotedText = rawFullText.contains("'") || rawFullText.contains("\"") || rawFullText.contains("‘") || rawFullText.contains("’");

                            if (!isInvalidTopText && !isQuotedText && !(!isAgreeOnlyTarget && isConsentChoiceOnly(cleanFullText))) {
                                if (rawFullText.contains(">") || rawFullText.contains(">>") || rawFullText.contains("->") || rawFullText.toLowerCase().contains("arrow_right") || rawFullText.toLowerCase().contains("arrowright"))
                                    isMatch = true;
                                if (cleanFullText.matches("^(continue|next|submit|tieptuc|tieptheo|tiepthe|trangtieptheo|tieptheo>|done|gui|send|batdau|agreeandcontinue|dongyvabatdau|>|>>|>>>|gotonextquestion|fwd|forward|tiep|arrowright)$"))
                                    isMatch = true;
                                // Tóm sống cái nút chuyển thẻ của bọn GfK/NIQ
                                if (resId.toLowerCase().contains("next") || resId.toLowerCase().contains("continue") || resId.toLowerCase().contains("btn_forward") || resId.toLowerCase().contains("navright"))
                                    isMatch = true;
                            }
                        }

                        // 2. Khớp theo AI tùy chỉnh
                        if (!isMatch && !targetNorm.isEmpty())
                        {
                            if (isArrow && rawFullText.contains(targetNorm))
                                isMatch = true;
                            if (!isArrow)
                            {
                                xmlCustomScore = getCustomClickButtonTextScore(cleanFullText, targetNorm);
                                if (xmlCustomScore != Integer.MIN_VALUE)
                                {
                                    boolean xmlClickable = node.getAttribute("clickable").equals("true");
                                    boolean isXmlBtnClass = node.getAttribute("class").contains("Button") || node.getAttribute("class").contains("Image");

                                    android.graphics.Rect rTest = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                    boolean isBigCard = rTest != null && rTest.width() >= 80 && rTest.height() >= 80 && rTest.width() < widthOfScreen * 0.9;
                                    boolean notParagraph = cleanFullText.length() < targetNorm.length() + 25; // Chống cắn nhầm văn bản mô tả
                                    boolean exactOrContains = cleanFullText.equals(targetNorm)
                                            || (targetNorm.length() >= 2 && cleanFullText.contains(targetNorm) && cleanFullText.length() <= targetNorm.length() + 10)
                                            || (cleanFullText.length() >= Math.max(5, (int) Math.ceil(targetNorm.length() * 0.75)) && targetNorm.contains(cleanFullText));

                                    if (exactOrContains || xmlClickable || isXmlBtnClass || (isBigCard && notParagraph))
                                        isMatch = true;
                                }
                            }
                        }

                        if (isMatch)
                        {
                            android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                            if (r != null)
                            {
                                boolean isRealArrow = cleanFullText.contains("arrowright") || resId.toLowerCase().contains("navright");
                                boolean isGiantNode = r.height() > 800; // Không có cái nút nào cao tới 800px cả, chắc chắn là thẻ Container Layout

                                // 🌟 KIM BÀI MIỄN TỬ: Nếu đúng là mũi tên xịn thì thả cửa, bất chấp tọa độ!
                                // Nếu không phải mũi tên thì vẫn phải lọc gắt (kích thước > 30) để tránh nút Submit ẩn (rác)
                                boolean passFilter = !isGiantNode && (isRealArrow || (r.height() > 30 && r.width() > 30 && r.centerY() > 180 && r.centerY() < heightOfScreen - 50));

                                if (passFilter)
                                {
                                    if (isNextIntent) {
                                        // Buff 10.000 điểm cho mũi tên xịn để nó đánh bại mọi nút Next/Submit rác dưới đáy!
                                        int scoreY = r.centerY() + (isRealArrow ? 10000 : 0);

                                        if (scoreY > maxCenterY + 20) {
                                            maxCenterY = scoreY;
                                            bestXmlBtnRect = r;
                                        } else if (Math.abs(scoreY - maxCenterY) <= 20) {
                                            if (bestXmlBtnRect == null || r.centerX() > bestXmlBtnRect.centerX()) {
                                                maxCenterY = scoreY;
                                                bestXmlBtnRect = r;
                                            }
                                        }
                                    } else {
                                        // Bắt nút đáp án tùy chỉnh (FUJI)
                                        int candidateScore = xmlCustomScore == Integer.MIN_VALUE ? 0 : xmlCustomScore;
                                        String clazzLower = node.getAttribute("class").toLowerCase(Locale.US);
                                        if (node.getAttribute("clickable").equals("true")) candidateScore += 5000;
                                        if (clazzLower.contains("button") || clazzLower.contains("image")) candidateScore += 3000;
                                        candidateScore += Math.min(3000, Math.max(0, r.centerY()));
                                        if (r.height() > 400 || r.width() > widthOfScreen * 0.95) candidateScore -= 2500;

                                        if (candidateScore > bestXmlCustomScore) {
                                            bestXmlCustomScore = candidateScore;
                                            bestXmlBtnRect = r;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (bestXmlBtnRect != null)
                    {
                        updateNotificationContent("Đồng hóa Next: XML Bắt sống tại " + bestXmlBtnRect.centerX() + "," + bestXmlBtnRect.centerY());
                        int clickY = getVisibleClickYAvoidingBottomOverlay(bestXmlBtnRect, bottomOverlayTop);
                        smartList = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                        click(bestXmlBtnRect.centerX(), clickY, false);
                        if (!checkNextOK(smartList, step))
                        {
                            swipeToTop(slVuot, false);
                        }
                        break checkButtonAgainLoop;
                    }
                }
                catch (Exception ignored)
                {
                }

                // TẦNG 4: XỬ LÝ VUỐT (NHƯ CŨ)
                List<HSQTools.TextBlock> currentVisible = smartList.stream().filter(x -> x.y > 180).collect(Collectors.toList());
                if (HSQTools.areAlmostSame(tempCompare, currentVisible, 20))
                {
                    if (vuotLenLai == 0)
                    {
                        vuotLenLai++;
                        updateNotificationContent("Chạm đáy đéo thấy nút, quay xe cuộn lên tìm lại...");
                    }
                    else
                    {
                        // =======================================================
                        // 🌟 TẦNG 5: SOM - NHỜ AI SOI ẢNH TÌM NÚT (HÀNG RÀO CUỐI)
                        // Chỉ gọi khi Tầng 1-4 đều bó tay
                        // =======================================================
                        updateNotificationContent("Tầng 1-4 bó tay! Triệu hồi AI soi ảnh SoM...");

                        // Lọc chỉ lấy node nửa dưới màn hình (nút next thường ở dưới)
                        List<HSQTools.TextBlock> somTargets = currentVisible.stream()
                                .filter(x -> x.y > heightOfScreen * 0.3)
                                .collect(Collectors.toList());

                        // Bổ sung: quét XML tìm thêm các node clickable TRỐNG TEXT (nút icon)
                        try {
                            currentXml = HSQTools.getFlexibleXML();
                            javax.xml.parsers.DocumentBuilder somBuilder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
                            org.w3c.dom.Document somDoc = somBuilder.parse(new java.io.ByteArrayInputStream(currentXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                            org.w3c.dom.NodeList somNodes = somDoc.getElementsByTagName("node");

                            for (int i = 0; i < somNodes.getLength(); i++) {
                                org.w3c.dom.Element sn = (org.w3c.dom.Element) somNodes.item(i);
                                if (!"true".equals(sn.getAttribute("clickable"))) continue;
                                String snText = sn.getAttribute("text").trim();
                                String snDesc = sn.getAttribute("content-desc").trim();
                                android.graphics.Rect snR = HSQTools.parseBoundsFromXml(sn.getAttribute("bounds"));
                                if (snR == null || snR.width() <= 0 || snR.height() <= 0) continue;
                                if (snR.width() < 30 || snR.height() < 30) continue;
                                if (snR.centerY() < heightOfScreen * 0.3 || snR.centerY() > heightOfScreen - 50) continue;

                                // Thêm node clickable (kể cả trống text) mà chưa có trong danh sách OCR
                                String label = snText.isEmpty() ? (snDesc.isEmpty() ? "[btn]" : snDesc) : snText;
                                boolean alreadyExists = somTargets.stream()
                                        .anyMatch(t -> Math.abs(t.y - snR.centerY()) < 40 && Math.abs(t.x - snR.centerX()) < 40);
                                if (!alreadyExists) {
                                    somTargets.add(new HSQTools.TextBlock(label, snR.centerX(), snR.centerY()));
                                }
                            }
                        } catch (Exception ignored) {}

                        if (!somTargets.isEmpty() && somTargets.size() <= 30) {
                            android.graphics.Bitmap markedImage = generateSoMImage(somTargets);
                            if (markedImage != null) {
                                HSQFileHelper.deleteFile(imagePath);
                                HSQFileHelper.createFolder(imagePath);
                                delay(500);

                                String somFilePath = imagePath + "/screenCapa1.png";
                                saveBitmapToFile(markedImage, somFilePath);
                                markedImage.recycle();
                                delay(1000);

                                String promptToGPT = "Tao đang tìm nút để chuyển trang (Ví dụ: Tiếp tục, Next, Continue, >>, Submit, Gửi, mũi tên...). " +
                                        "Trên ảnh tao đã đánh số màu đỏ. Mày hãy chọn đúng 1 con số đại diện cho cái nút đó. " +
                                        "LUẬT SINH TỬ 1: CẤM ĐƯỢC CHỌN VÀO CÁC CHỮ NHƯ 'Next', 'Tiếp theo' NẰM BÊN TRONG MỘT ĐOẠN VĂN BẢN/CÂU HƯỚNG DẪN DÀI. HÃY TÌM NÚT BẤM (BUTTON/ICON) THẬT SỰ HOẶC BIỂU TƯỢNG MŨI TÊN (>) ĐỂ CHUYỂN TRANG. " +
                                        "LUẬT SINH TỬ 2: NẾU TRÊN ẢNH ĐÉO CÓ SỐ NÀO TRÊN NÚT NEXT THẬT SỰ, MÀY PHẢI TRẢ VỀ TỌA ĐỘ CỦA NÚT ĐÓ BẰNG CÚ PHÁP: begin|som_chot|step1 click_point {X,Y};|end (với X,Y là tọa độ pixel, chiều ngang ảnh là 1440, dọc 3040). " +
                                        "NẾU CÓ SỐ NẰM TRÊN NÚT NEXT THẬT SỰ, HÃY TRẢ VỀ CÚ PHÁP LÀ: begin|som_chot|step1 click_som {số};|end . " +
                                        "NẾU CẢ MÀN HÌNH ĐÉO CÓ BẤT CỨ NÚT NEXT/MŨI TÊN NÀO ĐỂ BẤM (HOẶC CHỈ CÓ TEXT TRONG ĐOẠN VĂN), TRẢ VỀ SỐ 0. " +
                                        "CHỈ IN RA ĐÚNG 1 LỆNH DUY NHẤT, CẤM GIẢI THÍCH.";

                                String gptResponse = getAnswerFromGemByApi(1, false, false, promptToGPT);

                                int aiChoice = -1;
                                android.graphics.Point rawPoint = null;
                                try {
                                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("click_som\\s*\\{([0-9]+)\\}").matcher(gptResponse);
                                    if (m.find()) {
                                        aiChoice = Integer.parseInt(m.group(1).trim());
                                    } else {
                                        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("click_point\\s*\\{([0-9]+)\\s*,\\s*([0-9]+)\\}").matcher(gptResponse);
                                        if (m2.find()) {
                                            rawPoint = new android.graphics.Point(Integer.parseInt(m2.group(1).trim()), Integer.parseInt(m2.group(2).trim()));
                                        } else {
                                            // Fallback: thử bóc số thuần
                                            java.util.regex.Matcher numM = java.util.regex.Pattern.compile("\\d+").matcher(gptResponse);
                                            if (numM.find()) aiChoice = Integer.parseInt(numM.group().trim());
                                        }
                                    }
                                } catch (Exception ignored) {}

                                if (rawPoint != null) {
                                    updateNotificationContent("SoM ClickBtn: AI dùng tọa độ gốc X=" + rawPoint.x + ", Y=" + rawPoint.y);
                                    click(rawPoint.x, rawPoint.y, false);
                                    if (!checkNextOK(currentVisible, step)) {
                                        swipeToTop(slVuot, false);
                                    }
                                    break checkButtonAgainLoop;
                                } else if (aiChoice > 0 && somMap.containsKey(aiChoice)) {
                                    android.graphics.Point target = somMap.get(aiChoice);
                                    updateNotificationContent("SoM ClickBtn: AI chốt #" + aiChoice + " tại X=" + target.x + ", Y=" + target.y);
                                    click(target.x, target.y, false);
                                    if (!checkNextOK(currentVisible, step)) {
                                        swipeToTop(slVuot, false);
                                    }
                                    break checkButtonAgainLoop;
                                }
                            }
                        }

                        return currentVisible; // SOM cũng bó tay → FAIL
                    }
                }

                tempCompare = new ArrayList<>(currentVisible);

                if (vuotLenLai == 0)
                {
                    smartScroll(ysBot, ysTop, "clickbutton");
                }
                else
                {
                    smartScroll(ysTop, ysBot, "clickbutton");
                    slVuot++;
                }

                delay(2000);
            }
        }
        return null;
    }

    private int getClickButtonOcrMatchScore(HSQTools.TextBlock block, String targetNorm, boolean isArrow, boolean isNextIntent, boolean isAgreeOnlyTarget)
    {
        if (block == null || block.y <= 180 || block.text == null) return Integer.MIN_VALUE;

        String rawText = block.text.trim();
        String cleanText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawText));
        if (rawText.isEmpty()) return Integer.MIN_VALUE;

        if (isNextIntent && !isAgreeOnlyTarget && isConsentChoiceOnly(cleanText)) return Integer.MIN_VALUE;

        if (isNextIntent)
        {
            if (rawText.contains("'") || rawText.contains("\"") || rawText.contains("‘") || rawText.contains("’")) return Integer.MIN_VALUE;
            if (block.y < heightOfScreen * 0.2) return Integer.MIN_VALUE;

            if (rawText.equals(">") || rawText.equals(">>") || rawText.equals("->") || rawText.equals("=>") || rawText.contains("→"))
                return 100000 + block.y;
            if (cleanText.matches("^(continue|next|submit|tieptuc|tieptheo|tiepthe|trangtieptheo|tieptheo>|done|gui|send|batdau|agreeandcontinue|dongyvabatdau|>|>>|>>>|gotonextquestion|fwd|forward|tiep|arrowright)$"))
                return 90000 + block.y;
        }

        if (targetNorm == null || targetNorm.isEmpty()) return Integer.MIN_VALUE;

        if (isArrow)
        {
            if (rawText.equals(targetNorm) || rawText.contains(targetNorm)) return 80000 + block.y;
            return Integer.MIN_VALUE;
        }

        int customScore = getCustomClickButtonTextScore(cleanText, targetNorm);
        if (customScore == Integer.MIN_VALUE) return Integer.MIN_VALUE;

        // Non-next custom buttons should prefer the real button lower on the page over a heading above it.
        return customScore + Math.min(3000, Math.max(0, block.y));
    }

    private int getCustomClickButtonTextScore(String cleanText, String targetNorm)
    {
        if (cleanText == null || targetNorm == null) return Integer.MIN_VALUE;
        if (cleanText.isEmpty() || targetNorm.isEmpty()) return Integer.MIN_VALUE;

        int lenDiff = Math.abs(cleanText.length() - targetNorm.length());
        if (cleanText.equals(targetNorm)) return 100000 - lenDiff;

        if (targetNorm.length() >= 2 && cleanText.contains(targetNorm) && cleanText.length() <= targetNorm.length() + 10)
            return 90000 - lenDiff;

        int minContainedLen = Math.max(5, (int) Math.ceil(targetNorm.length() * 0.75));
        if (cleanText.length() >= minContainedLen && targetNorm.contains(cleanText))
            return 78000 - lenDiff;

        if (!isSafeFuzzyClickButtonMatch(cleanText, targetNorm)) return Integer.MIN_VALUE;

        int dist = HSQTools.levenshtein(cleanText, targetNorm);
        return 60000 - (dist * 1000) - (lenDiff * 20);
    }

    private boolean isSafeFuzzyClickButtonMatch(String cleanText, String targetNorm)
    {
        if (cleanText == null || targetNorm == null) return false;
        if (cleanText.length() < 6 || targetNorm.length() < 6) return false;

        int lenDiff = Math.abs(cleanText.length() - targetNorm.length());
        if (lenDiff > Math.max(2, (int) (targetNorm.length() * 0.25))) return false;

        int prefixLen = Math.min(3, Math.min(cleanText.length(), targetNorm.length()));
        if (!cleanText.substring(0, prefixLen).equals(targetNorm.substring(0, prefixLen))) return false;

        int maxDist = Math.min(3, Math.max(1, (int) Math.floor(targetNorm.length() * 0.18)));
        return HSQTools.levenshtein(cleanText, targetNorm) <= maxDist;
    }

    private int tryClickPageNavigationFromXml(String step, List<HSQTools.TextBlock> smartList, int slVuot, boolean canReveal)
    {
        try
        {
            String xml = HSQTools.getFlexibleXML();
            if (xml == null || xml.trim().isEmpty()) return 0;

            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");
            int bottomOverlayTop = findLikelyBottomOverlayTop(nodes);

            android.graphics.Rect bestRect = null;
            String bestLabel = "";
            boolean bestClipped = false;
            int bestScore = Integer.MIN_VALUE;

            for (int i = 0; i < nodes.getLength(); i++)
            {
                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                if ("false".equals(node.getAttribute("enabled"))) continue;

                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                if (r == null) continue;

                String text = node.getAttribute("text");
                String desc = node.getAttribute("content-desc");
                String resId = node.getAttribute("resource-id");
                String clazz = node.getAttribute("class");
                String clickable = node.getAttribute("clickable");
                String focusable = node.getAttribute("focusable");

                String rawFullText = ((text == null ? "" : text) + " " + (desc == null ? "" : desc)).trim();
                String cleanFullText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawFullText));
                if (isConsentChoiceOnly(cleanFullText)) continue;
                if (isLikelyFloatingScrollTopControl(r, rawFullText + " " + resId + " " + clazz, smartList)) continue;

                String resLower = resId == null ? "" : resId.toLowerCase(Locale.US);
                if (resLower.contains("statusbarbackground") || resLower.contains("navigationbarbackground")) continue;
                String clazzLower = clazz == null ? "" : clazz.toLowerCase(Locale.US);
                boolean navId = resLower.contains("nav") || resLower.contains("next") || resLower.contains("continue") || resLower.contains("submit");
                boolean nextText = isNextNavigationText(rawFullText, cleanFullText);
                int visibleBottomBeforeOverlay = bottomOverlayTop > 0 ? Math.min(r.bottom, bottomOverlayTop - 8) : r.bottom;
                int visibleHeightBeforeOverlay = Math.max(0, visibleBottomBeforeOverlay - Math.max(r.top, 181));
                boolean clippedByBottomOverlay = bottomOverlayTop > 0 && r.bottom > bottomOverlayTop && visibleHeightBeforeOverlay < 24;
                android.graphics.Rect ancestorVisibleRect = getVisibleRectInsideXmlAncestors(node, r);
                int ancestorVisibleHeight = Math.max(0, ancestorVisibleRect.bottom - ancestorVisibleRect.top);
                boolean clippedByXmlAncestor = ancestorVisibleHeight < 24 || r.centerY() < ancestorVisibleRect.top || r.centerY() > ancestorVisibleRect.bottom;
                boolean clipped = r.height() < 24 || (r.bottom >= heightOfScreen - 80 && r.height() < 80) || r.centerY() >= heightOfScreen - 40 || clippedByBottomOverlay || clippedByXmlAncestor;
                boolean navOnlyReveal = navId && !nextText && clipped;
                if (!nextText && !navOnlyReveal) continue;

                boolean buttonish = clazzLower.contains("button")
                        || clazzLower.contains("image")
                        || "true".equals(clickable)
                        || "true".equals(focusable)
                        || navId;
                if (!buttonish) continue;

                if (r.height() > 800) continue;
                if (!clipped)
                {
                    if (r.centerY() <= 180 || r.centerY() >= heightOfScreen - 80) continue;
                    if (r.width() < 30 || r.height() < 30) continue;
                    if (navId && !"true".equals(clickable) && !clazzLower.contains("button") && r.width() > widthOfScreen * 0.45)
                        continue;
                }

                int score = r.centerY();
                if (nextText) score += 5000;
                if (clazzLower.contains("button")) score += 2500;
                if ("true".equals(clickable)) score += 1800;
                if (navId) score += 1200;
                if (resLower.contains("nav-container") || resLower.contains("tblnav") || resLower.endsWith("navigation")) score += 1200;
                if (cleanFullText.matches("^(tieptheo|tiepthe|xinhaytieptuc|haytieptuc|vuilongtieptuc|next|continue|submit|trangtieptheo|batdau|dongyvabatdau|agreeandcontinue)$")) score += 1800;
                if (r.centerX() > widthOfScreen / 2) score += 500;
                if (clipped) score += 700;

                if (score > bestScore)
                {
                    bestScore = score;
                    bestRect = r;
                    bestLabel = rawFullText.isEmpty() ? resId : rawFullText;
                    bestClipped = clipped;
                }
            }

            if (bestRect == null) return 0;

            if (bestClipped)
            {
                List<HSQTools.TextBlock> beforeReveal = smartList == null
                        ? getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList())
                        : smartList.stream().filter(x -> x.y > 180).collect(Collectors.toList());

                if (canReveal)
                {
                    updateNotificationContent("XML Nav: nut [" + bestLabel + "] dang ket duoi day, vuot xuong de lo nut");
                    smartScroll(ysBot, ysTop, "xmlnav_reveal");
                    delay(1200);

                    List<HSQTools.TextBlock> afterReveal = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                    if (!HSQTools.areAlmostSame(beforeReveal, afterReveal, 20))
                    {
                        return 2;
                    }

                    updateNotificationContent("XML Nav: da cham day, thu bam nut [" + bestLabel + "]");
                }
                else
                {
                    updateNotificationContent("XML Nav: het luot reveal, thu bam nut [" + bestLabel + "]");
                }
            }

            int clickX = Math.max(24, Math.min(widthOfScreen - 24, bestRect.centerX()));
            int clickY = getVisibleClickYAvoidingBottomOverlay(bestRect, bottomOverlayTop);
            updateNotificationContent("XML Nav: bam nut [" + bestLabel + "] tai " + clickX + "," + clickY);

            List<HSQTools.TextBlock> beforeClick = smartList == null
                    ? getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList())
                    : smartList.stream().filter(x -> x.y > 180).collect(Collectors.toList());
            click(clickX, clickY, false);
            if (!checkNextOK(beforeClick, step))
            {
                swipeToTop(slVuot, false);
                if (canReveal && !bestClipped)
                {
                    updateNotificationContent("XML Nav: bam chua an, vuot de lo nut roi thu lai");
                    smartScroll(ysBot, ysTop, "xmlnav_retry_reveal");
                    return 2;
                }
                return 0;
            }
            return 1;
        }
        catch (Exception ignored)
        {
            return 0;
        }
    }

    private android.graphics.Rect getVisibleRectInsideXmlAncestors(org.w3c.dom.Element node, android.graphics.Rect childRect)
    {
        android.graphics.Rect visible = new android.graphics.Rect(childRect);
        try
        {
            org.w3c.dom.Node parent = node == null ? null : node.getParentNode();
            while (parent instanceof org.w3c.dom.Element)
            {
                org.w3c.dom.Element parentElement = (org.w3c.dom.Element) parent;
                android.graphics.Rect parentRect = HSQTools.parseBoundsFromXml(parentElement.getAttribute("bounds"));
                if (parentRect != null && parentRect.width() > 0 && parentRect.height() > 0)
                {
                    int left = Math.max(visible.left, parentRect.left);
                    int top = Math.max(visible.top, parentRect.top);
                    int right = Math.min(visible.right, parentRect.right);
                    int bottom = Math.min(visible.bottom, parentRect.bottom);
                    visible.set(left, top, right, bottom);
                    if (visible.right <= visible.left || visible.bottom <= visible.top)
                    {
                        return new android.graphics.Rect(visible.left, visible.top, visible.left, visible.top);
                    }
                }
                parent = parent.getParentNode();
            }
        }
        catch (Exception ignored)
        {
        }
        return visible;
    }

    private boolean isNextNavigationText(String rawFullText, String cleanFullText)
    {
        String rawLower = rawFullText == null ? "" : rawFullText.toLowerCase(Locale.US).replaceAll("\\s+", "");
        if (rawLower.contains(">") || rawLower.contains("->") || rawLower.contains("arrow_right") || rawLower.contains("arrowright")) return true;
        if (cleanFullText == null) cleanFullText = "";
        if (cleanFullText.matches("^(continue|next|submit|tieptuc|tieptheo|tiepthe|xinhaytieptuc|haytieptuc|vuilongtieptuc|trangtieptheo|tieptheotrang|done|gui|send|batdau|agreeandcontinue|dongyvabatdau|gotonextquestion|fwd|forward|tiep|arrowright)$")) return true;
        return cleanFullText.length() <= 80 && (cleanFullText.contains("tieptuc") || cleanFullText.contains("tieptheo") || cleanFullText.contains("tiepthe") || cleanFullText.contains("next") || cleanFullText.contains("continue") || cleanFullText.contains("submit") || cleanFullText.contains("batdau"));
    }

    private boolean isLikelyFloatingScrollTopControl(android.graphics.Rect r, String rawMeta, List<HSQTools.TextBlock> smartList)
    {
        if (r == null) return false;

        boolean bottomRight = r.centerX() > widthOfScreen * 0.66f && r.centerY() > heightOfScreen * 0.68f;
        int minSide = Math.min(r.width(), r.height());
        int maxSide = Math.max(r.width(), r.height());
        boolean squareLike = minSide >= 80 && maxSide <= 360 && ((float) maxSide / Math.max(1, minSide)) <= 1.65f;
        if (!bottomRight || !squareLike) return false;

        String rawLower = rawMeta == null ? "" : rawMeta.toLowerCase(Locale.US).replaceAll("\\s+", "");
        String cleanMeta = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawMeta == null ? "" : rawMeta));
        boolean upMeta = rawLower.contains("arrow_up")
                || rawLower.contains("arrowup")
                || rawLower.contains("keyboard_arrow_up")
                || rawLower.contains("expand_less")
                || rawLower.contains("scrolltop")
                || rawLower.contains("backtotop")
                || cleanMeta.contains("cuonlen")
                || cleanMeta.contains("lentop")
                || cleanMeta.contains("vedau")
                || cleanMeta.contains("gotop");

        boolean hasNextTextLeft = false;
        if (smartList != null)
        {
            for (HSQTools.TextBlock block : smartList)
            {
                if (block == null || block.text == null) continue;
                if (block.x >= r.centerX() - 50) continue;
                if (block.y < r.top - 160 || block.y > r.bottom + 160) continue;
                String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(block.text));
                if (clean.matches("^(tieptheo|tiepthe|tieptuc|next|continue|submit|trangtieptheo)$")
                        || clean.contains("tiepthe")
                        || clean.contains("tieptuc"))
                {
                    hasNextTextLeft = true;
                    break;
                }
            }
        }

        return upMeta || hasNextTextLeft;
    }
    private boolean isConsentChoiceOnly(String cleanText)
    {
        if (cleanText == null || cleanText.isEmpty()) return false;
        return cleanText.matches("^(dongy|toidongy|khongdongy|toikhongdongy|agree|iagree|disagree|donotagree|khongchapnhan|privacypolicy)$");
    }
    private int findLikelyBottomOverlayTop(org.w3c.dom.NodeList nodes)
    {
        if (nodes == null) return -1;

        int bestTop = Integer.MAX_VALUE;
        for (int i = 0; i < nodes.getLength(); i++)
        {
            org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
            android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
            if (r == null) continue;
            if (r.top < heightOfScreen * 0.55 || r.width() < widthOfScreen * 0.55 || r.height() < 50) continue;

            String text = node.getAttribute("text");
            String desc = node.getAttribute("content-desc");
            String resId = node.getAttribute("resource-id");
            String clazz = node.getAttribute("class");
            String raw = ((text == null ? "" : text) + " "
                    + (desc == null ? "" : desc) + " "
                    + (resId == null ? "" : resId) + " "
                    + (clazz == null ? "" : clazz)).toLowerCase(Locale.US);
            String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(raw));

            boolean bottomNavText = clean.contains("home") && clean.contains("surveys") && clean.contains("menu");
            boolean bottomNavMeta = raw.contains("bottomnavigation") || raw.contains("bottom_nav") || raw.contains("bottomnav") || raw.contains("tablayout");
            boolean androidNavBar = raw.contains("navigationbarbackground");
            if (bottomNavText || bottomNavMeta || androidNavBar)
            {
                bestTop = Math.min(bestTop, r.top);
            }
        }

        return bestTop == Integer.MAX_VALUE ? -1 : bestTop;
    }

    private int getVisibleClickYAvoidingBottomOverlay(android.graphics.Rect r, int bottomOverlayTop)
    {
        int clickY = r.centerY();
        if (bottomOverlayTop > 0 && r.top < bottomOverlayTop && r.bottom > bottomOverlayTop)
        {
            int visibleTop = Math.max(181, r.top);
            int visibleBottom = Math.min(r.bottom, bottomOverlayTop - 12);
            if (visibleBottom > visibleTop)
            {
                clickY = (visibleTop + visibleBottom) / 2;
            }
        }

        return Math.max(181, Math.min(heightOfScreen - 120, clickY));
    }
    private boolean tryClickWebSurveyVisualArrowFallback(String step, List<HSQTools.TextBlock> smartList, int slVuot)
    {
        try
        {
            String xml = HSQTools.getFlexibleXML();
            if (xml == null || xml.trim().isEmpty()) return false;

            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

            boolean hasRightArrowInstruction = false;
            android.graphics.Rect selectedChipRect = null;
            android.graphics.Rect firstCardRect = null;
            android.graphics.Rect navControlsRect = null;

            for (int i = 0; i < nodes.getLength(); i++)
            {
                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                String resId = node.getAttribute("resource-id");
                String clazz = node.getAttribute("class");
                String text = node.getAttribute("text");
                String clickable = node.getAttribute("clickable");
                String selected = node.getAttribute("selected");
                String checkable = node.getAttribute("checkable");
                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                if (r == null) continue;

                String resLower = resId == null ? "" : resId.toLowerCase();
                if (resLower.contains("nav-controls"))
                {
                    navControlsRect = r;
                }

                if (resLower.contains("question"))
                {
                    String question = text == null ? "" : text;
                    String cleanQuestion = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(question));
                    if (question.contains(">") || question.contains("&gt;") || cleanQuestion.contains("benphaidetieptuc"))
                    {
                        hasRightArrowInstruction = true;
                    }
                }

                if ("true".equals(clickable)
                        && "true".equals(selected)
                        && r.centerY() > 300
                        && r.centerY() < heightOfScreen - 500
                        && r.height() >= 40
                        && r.height() <= 180
                        && r.width() <= Math.max(220, widthOfScreen / 5))
                {
                    if (selectedChipRect == null || r.centerY() > selectedChipRect.centerY())
                    {
                        selectedChipRect = r;
                    }
                }

                if ("true".equals(checkable)
                        && clazz.contains("CheckBox")
                        && r.width() > widthOfScreen * 0.25
                        && r.height() > 140
                        && r.top > 300
                        && r.top < heightOfScreen - 40)
                {
                    if (firstCardRect == null || r.top < firstCardRect.top)
                    {
                        firstCardRect = r;
                    }
                }
            }

            if (!hasRightArrowInstruction) return false;

            int clickX;
            int clickY;
            int visualTop;
            int visualBottom;

            if (navControlsRect != null && navControlsRect.width() > widthOfScreen * 0.4 && navControlsRect.height() > 30)
            {
                clickX = Math.min(navControlsRect.right - 35, (int) (widthOfScreen * 0.94));
                clickY = navControlsRect.centerY();
                visualTop = navControlsRect.top;
                visualBottom = navControlsRect.bottom;
            }
            else if (selectedChipRect != null && firstCardRect != null)
            {
                int gapTop = selectedChipRect.bottom;
                int gapBottom = firstCardRect.top;
                int gap = gapBottom - gapTop;
                if (gap < 80 || gap > 420) return false;

                clickX = Math.min(widthOfScreen - 90, (int) (widthOfScreen * 0.93));
                clickY = gapTop + (gap / 2);
                visualTop = gapTop;
                visualBottom = gapBottom;
            }
            else
            {
                return false;
            }

            if (clickX <= widthOfScreen / 2 || clickY <= 180 || clickY >= heightOfScreen - 80) return false;

            android.graphics.Point visualChevron = findBrightRightChevronPoint(visualTop, visualBottom);
            if (visualChevron != null)
            {
                clickX = visualChevron.x;
                clickY = visualChevron.y;
                updateNotificationContent("Dong hoa Next the: bitmap thay mui ten tai " + clickX + "," + clickY);
            }
            else
            {
                updateNotificationContent("Dong hoa Next the: fallback layout tai " + clickX + "," + clickY);
            }

            List<HSQTools.TextBlock> beforeClick = smartList == null
                    ? getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList())
                    : smartList.stream().filter(x -> x.y > 180).collect(Collectors.toList());
            click(clickX, clickY, false);
            if (!checkNextOK(beforeClick, step))
            {
                swipeToTop(slVuot, false);
            }
            return true;
        }
        catch (Exception ignored)
        {
            return false;
        }
    }
    private android.graphics.Point findBrightRightChevronPoint(int top, int bottom)
    {
        try
        {
            android.graphics.Bitmap bitmap = HSQTools.getScreenBitmap();
            if (bitmap == null) return null;

            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int scanLeft = Math.max((int) (w * 0.78), 0);
            int scanRight = Math.min(w - 8, Math.max(scanLeft + 1, w - 20));
            int scanTop = Math.max(180, Math.min(top, bottom));
            int scanBottom = Math.min(h - 80, Math.max(top, bottom));
            if (scanBottom - scanTop < 35) return null;

            long sumX = 0;
            long sumY = 0;
            int count = 0;
            int minX = w;
            int maxX = 0;
            int minY = h;
            int maxY = 0;

            for (int y = scanTop; y < scanBottom; y += 2)
            {
                for (int x = scanLeft; x < scanRight; x += 2)
                {
                    int c = bitmap.getPixel(x, y);
                    int r = (c >> 16) & 0xff;
                    int g = (c >> 8) & 0xff;
                    int b = c & 0xff;
                    int lum = (r * 299 + g * 587 + b * 114) / 1000;
                    int max = Math.max(r, Math.max(g, b));
                    int min = Math.min(r, Math.min(g, b));

                    if (lum < 210 || max < 225) continue;
                    if (max - min > 90) continue;

                    int bgX = Math.max(0, x - 18);
                    int bg = bitmap.getPixel(bgX, y);
                    int br = (bg >> 16) & 0xff;
                    int bgc = (bg >> 8) & 0xff;
                    int bb = bg & 0xff;
                    int bgLum = (br * 299 + bgc * 587 + bb * 114) / 1000;
                    if (lum - bgLum < 18 && x < w * 0.88) continue;

                    sumX += x;
                    sumY += y;
                    count++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }

            if (count < 35) return null;
            if (maxX - minX < 12 || maxY - minY < 20) return null;

            int cx = (int) (sumX / count);
            int cy = (int) (sumY / count);
            if (cx <= w / 2 || cy <= 180 || cy >= h - 80) return null;
            return new android.graphics.Point(cx, cy);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }
    private boolean isDropdownNavigationButtonText(String clean)
    {
        if (clean == null || clean.isEmpty()) return false;
        return clean.matches("^(back|next|previous|continue|submit|done|finish|start|tiep|tieptuc|tieptheo|quaylai|trove|lui|dongy|batdau)$");
    }

    private boolean isLikelyHtmlSelectButton(String raw, String resId, String clazz, android.graphics.Rect rect)
    {
        if (rect == null) return false;
        String lowerRaw = raw == null ? "" : raw.toLowerCase(Locale.US);
        String lowerId = resId == null ? "" : resId.toLowerCase(Locale.US);
        String className = clazz == null ? "" : clazz;
        boolean buttonLike = className.contains("Button") || className.contains("Spinner") || className.contains("AutoComplete");
        boolean selectMeta = lowerId.startsWith("radix-")
                || lowerId.contains("select")
                || lowerId.contains("dropdown")
                || lowerId.contains("spinner")
                || lowerRaw.contains("<table")
                || lowerRaw.contains("&lt;table")
                || lowerRaw.contains("select an option")
                || lowerRaw.contains("please select");
        return buttonLike && selectMeta && rect.width() >= widthOfScreen * 0.35f && rect.height() >= 45;
    }
    private List<TextBlock> clickDropDown(String contextStr)
    {
        int vuotTimKiem = 0;
        int vuotLenLai = 0;
        int daVuotLen = 0;
        String xml = "";
        List<TextBlock> currentScreen = getCheckAnswerSmart();
        phoneScreen.addAll(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
        phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));

        class XmlNodeTemp
        {
            android.graphics.Rect rect;
            String rawText;
            String rawDesc;
            String resId;
            String clazz;
            String cleanCombined;
            String rawCompact;
            boolean clickable;
            boolean focusable;

            XmlNodeTemp(org.w3c.dom.Element node)
            {
                rawText = node.getAttribute("text");
                rawDesc = node.getAttribute("content-desc");
                resId = node.getAttribute("resource-id");
                clazz = node.getAttribute("class");
                clickable = "true".equals(node.getAttribute("clickable"));
                focusable = "true".equals(node.getAttribute("focusable"));
                rect = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                String combined = ((rawText == null ? "" : rawText) + " " + (rawDesc == null ? "" : rawDesc)).trim();
                cleanCombined = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(combined));
                rawCompact = combined.replaceAll("\\s+", "");
            }
        }

        while (true)
        {
            String normTarget = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(contextStr));
            if (normTarget.isEmpty()) return currentScreen;

            HSQTools.TextBlock exactTextNode = currentScreen.stream()
                    .filter(x -> x.y > 180)
                    .filter(x ->
                    {
                        String cleanText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text));
                        if (cleanText == null || cleanText.isEmpty()) return false;

                        if (cleanText.equals(normTarget)) return true;
                        if (cleanText.contains(normTarget)) return true;
                        if (normTarget.contains(cleanText) && cleanText.length() >= 3) return true;

                        if (normTarget.length() >= 4 && cleanText.length() >= 3)
                        {
                            String t1 = normTarget.substring(0, Math.min(3, normTarget.length()));
                            String t2 = cleanText.substring(0, Math.min(3, cleanText.length()));
                            if (t1.equals(t2)) return true;
                        }

                        return HSQTools.levenshtein(cleanText, normTarget) <= Math.max(1, (int) (normTarget.length() * 0.2));
                    }).min(Comparator.comparingInt((TextBlock x) ->
                    {
                        String cleanText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text));
                        return cleanText.equals(normTarget) ? 0 : 1;
                    }).thenComparingInt(x -> Math.abs(x.text.length() - contextStr.length())))
                    .orElse(null);

            if (exactTextNode == null)
            {
                if (vuotLenLai < 2)
                {
                    swipe(xCenter, yBot, xCenter, yTop, 2000);
                    delay(2000);
                    currentScreen = getCheckAnswerSmart();
                    phoneScreen.addAll(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
                    phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));
                    vuotLenLai++;
                    continue;
                }
                return currentScreen;
            }

            if (exactTextNode.y > 2600 && daVuotLen == 0)
            {
                updateNotificationContent("Dropdown sat day, vuot len mot chut!");
                swipe(xCenter, yBot, xCenter, yBot - 600, 1000);
                delay(2000);
                currentScreen = getCheckAnswerSmart();
                phoneScreen.addAll(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
                phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));
                daVuotLen++;
                continue;
            }

            List<android.graphics.Point> candidates = new ArrayList<>();

            try
            {
                List<HSQTools.TextBlock> rawOcr = HSQTools.getOcrTextBlocks();
                HSQTools.TextBlock bestOcr = null;
                int bestOcrScore = Integer.MAX_VALUE;

                for (HSQTools.TextBlock n : rawOcr)
                {
                    if (n == null || n.text == null) continue;
                    if (n.y <= exactTextNode.y - 20 || n.y >= exactTextNode.y + 750) continue;
                    if (Math.abs(n.x - exactTextNode.x) >= widthOfScreen) continue;

                    String raw = n.text.trim();
                    String rawCompact = raw.replaceAll("\\s+", "");
                    String c = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(raw));

                    boolean isClassicDropdownText =
                            c.equals("v") || c.equals("chon") || c.equals("select") || c.equals("choose")
                                    || c.equals("chonmot") || c.equals("selectone") || c.contains("vuilongchon")
                                    || c.equals("luachon") || c.equals("vuilongluachon")
                                    || c.equals("haychonmotphuongan") || c.contains("pleaseselect")
                                    || raw.equals("-") || c.equals("-")
                                    || raw.equals("...") || c.equals("...");

                    boolean isMaskedDropdownPlaceholder = rawCompact.matches("^[=._\\-]{3,}$");

                    if (!isClassicDropdownText && !isMaskedDropdownPlaceholder) continue;

                    int score = Math.abs(n.y - exactTextNode.y) + (Math.abs(n.x - exactTextNode.x) / 4);
                    if (n.y > exactTextNode.y + 40) score -= 120;
                    if (isMaskedDropdownPlaceholder) score -= 240;

                    if (score < bestOcrScore)
                    {
                        bestOcrScore = score;
                        bestOcr = n;
                    }
                }

                if (bestOcr != null)
                {
                    candidates.add(new android.graphics.Point(bestOcr.x, bestOcr.y));
                }
            }
            catch (Exception ignored) {}

            try
            {
                xml = HSQTools.getFlexibleXML();
                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                List<XmlNodeTemp> xmlNodes = new ArrayList<>();
                for (int i = 0; i < nodes.getLength(); i++)
                {
                    org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                    XmlNodeTemp x = new XmlNodeTemp(node);
                    if (x.rect == null) continue;
                    if (x.rect.centerY() <= 180 || x.rect.centerY() >= heightOfScreen - 20) continue;
                    xmlNodes.add(x);
                }

                android.graphics.Rect tightLabelRect = null;
                int wideLabelBottom = exactTextNode.y + 60;

                for (XmlNodeTemp n : xmlNodes)
                {
                    if (n.cleanCombined.isEmpty()) continue;

                    boolean isMatch =
                            n.cleanCombined.equals(normTarget)
                                    || n.cleanCombined.contains(normTarget)
                                    || (normTarget.contains(n.cleanCombined) && n.cleanCombined.length() >= 3);

                    if (!isMatch) continue;
                    if (Math.abs(n.rect.centerY() - exactTextNode.y) > 220) continue;

                    wideLabelBottom = Math.max(wideLabelBottom, n.rect.bottom);

                    int area = n.rect.width() * n.rect.height();
                    if (tightLabelRect == null)
                    {
                        tightLabelRect = n.rect;
                    }
                    else
                    {
                        int oldArea = tightLabelRect.width() * tightLabelRect.height();
                        int oldDistance = Math.abs(tightLabelRect.centerY() - exactTextNode.y) + Math.abs(tightLabelRect.centerX() - exactTextNode.x);
                        int newDistance = Math.abs(n.rect.centerY() - exactTextNode.y) + Math.abs(n.rect.centerX() - exactTextNode.x);

                        if (area < oldArea || (area == oldArea && newDistance < oldDistance))
                        {
                            tightLabelRect = n.rect;
                        }
                    }
                }

                if (tightLabelRect == null)
                {
                    tightLabelRect = new android.graphics.Rect(
                            Math.max(0, exactTextNode.x - 160),
                            Math.max(0, exactTextNode.y - 60),
                            Math.min(widthOfScreen, exactTextNode.x + 160),
                            Math.min(heightOfScreen, exactTextNode.y + 60)
                    );
                }

                android.graphics.Rect bestDropdownRect = null;
                double bestScore = Double.MAX_VALUE;

                for (XmlNodeTemp n : xmlNodes)
                {
                    android.graphics.Rect r = n.rect;
                    if (r.width() < 100 || r.height() < 35 || r.height() > 320) continue;

                    boolean classLooksInput =
                            n.clazz.contains("Spinner")
                                    || n.clazz.contains("EditText")
                                    || n.clazz.contains("AutoComplete")
                                    || n.clazz.contains("Button");

                    String rawForCheck = n.rawText == null ? "" : n.rawText.trim();
                    String clean = n.cleanCombined;

                    boolean sameTextAsLabel =
                            !clean.isEmpty()
                                    && (clean.equals(normTarget) || (clean.contains(normTarget) && clean.length() <= normTarget.length() + 2));
                    String rid = n.resId == null ? "" : n.resId.toLowerCase(Locale.US);
                    boolean isNavButtonText = n.clazz.contains("Button") && isDropdownNavigationButtonText(clean);
                    boolean isHtmlSelectButton = isLikelyHtmlSelectButton(n.rawText, n.resId, n.clazz, r);
                    if (isNavButtonText && !isHtmlSelectButton) continue;

                    // 🔥 BẮT MẠCH AN TOÀN: Nếu đích thị là Input xịn (Spinner/EditText) thì mới ưu tiên cao
                    if (sameTextAsLabel && classLooksInput) {
                        bestDropdownRect = r;
                        break; // Nghỉ tính điểm lằng nhằng, ăn luôn!
                    }

                    boolean isInteractive = n.clickable || n.focusable || classLooksInput;

                    boolean isClassicDropdownText =
                            clean.equals("v") || clean.equals("chon") || clean.equals("select") || clean.equals("choose")
                                    || clean.equals("chonmot") || clean.equals("selectone") || clean.contains("vuilongchon")
                                    || clean.equals("luachon") || clean.equals("vuilongluachon")
                                    || clean.equals("haychonmotphuongan") || clean.contains("pleaseselect")
                                    || rawForCheck.equals("-") || clean.equals("-")
                                    || rawForCheck.equals("...") || clean.equals("...");

                    boolean isMaskedDropdownPlaceholder = n.rawCompact.matches("^[=._\\-]{3,}$");

                    boolean belowLabel =
                            r.top >= (wideLabelBottom - 15)
                                    && r.top <= (wideLabelBottom + 900)
                                    && (Math.abs(r.centerX() - exactTextNode.x) < widthOfScreen
                                    || Math.abs(r.left - exactTextNode.x) < widthOfScreen
                                    || (r.left <= tightLabelRect.right + 260 && r.right >= tightLabelRect.left - 120));

                    boolean inlineRight =
                            Math.abs(r.centerY() - exactTextNode.y) <= 500
                                    && r.centerX() > exactTextNode.x + 30;

                    // 🌟 FIX: Bọc thép cho trường hợp Dropdown bao trọn Text
                    boolean surroundsText = r.contains(exactTextNode.x, exactTextNode.y);
                    boolean isSelf = sameTextAsLabel || surroundsText;

                    if (!belowLabel && !inlineRight && !isSelf) continue;

                    // Thêm n.clickable vào để cứu các Dropdown mang class ẩn danh (View)
                    if (sameTextAsLabel && !isClassicDropdownText && !isMaskedDropdownPlaceholder && !n.focusable && !classLooksInput && !n.clickable)
                        continue;

                    if (!isInteractive && !isClassicDropdownText && !isMaskedDropdownPlaceholder)
                        continue;

                    if (!isHtmlSelectButton && !clean.isEmpty() && clean.length() > 70) continue;

                    double score = (Math.abs(r.top - wideLabelBottom) * 1.3) + (Math.abs(r.centerX() - exactTextNode.x) * 0.25);

                    if (belowLabel) score -= 80;
                    if (inlineRight) score -= 120;
                    if (isMaskedDropdownPlaceholder) score -= 700;
                    if (isClassicDropdownText) score -= 450;
                    if (n.focusable) score -= 250;
                    if (classLooksInput) score -= 160;
                    if (n.clickable) score -= 90;

                    if (rid.endsWith("_c") || rid.contains("spinner") || rid.contains("dropdown") || rid.contains("select") || rid.startsWith("qr~") || rid.startsWith("radix-"))
                        score -= 180;
                    if (isHtmlSelectButton) score -= 1800;

                    // 🌟 Bao trọn gói cả classLooksInput lẫn n.clickable
                    if (sameTextAsLabel) {
                        if (classLooksInput) score -= 2000;
                        else if (n.clickable) score -= 600; // Giảm nhẹ bonus của View có clickable ảo
                        else score += 950;
                    } else if (surroundsText) {
                        if (classLooksInput) score -= 1500;
                        else if (n.clickable) score -= 500;
                    }

                    // 🌟 FIX: Loại bỏ nút Submit/Next (Button text rỗng nằm xa label)
                    if (n.clazz.contains("Button") && clean.isEmpty() && Math.abs(r.top - exactTextNode.y) > 200) continue;

                    if (r.width() > widthOfScreen - 40 && Math.abs(r.top - exactTextNode.y) > 300) continue;

                    if (r.centerY() <= exactTextNode.y + 60 && !inlineRight && !surroundsText && !sameTextAsLabel) score += 500;

                    if (score < bestScore)
                    {
                        bestScore = score;
                        bestDropdownRect = r;
                    }
                }

                if (bestDropdownRect != null)
                {
                    int safeX = bestDropdownRect.left + (int) (bestDropdownRect.width() * 0.82);
                    if (safeX > bestDropdownRect.right - 20) safeX = bestDropdownRect.centerX();

                    // Bơm tọa độ mép phải (82%)
                    candidates.add(new android.graphics.Point(safeX, bestDropdownRect.centerY()));

                    // 🌟 FIX: Bơm thêm tọa độ chính giữa (Center) để gõ bồi nếu WebView bị củ chuối
                    boolean centerExisted = false;
                    for (android.graphics.Point oldPt : candidates) {
                        if (Math.abs(oldPt.x - bestDropdownRect.centerX()) < 60 && Math.abs(oldPt.y - bestDropdownRect.centerY()) < 60) {
                            centerExisted = true; break;
                        }
                    }
                    if (!centerExisted) {
                        candidates.add(new android.graphics.Point(bestDropdownRect.centerX(), bestDropdownRect.centerY()));
                    }
                }
            }
            catch (Exception ignored) {}

            try
            {
                android.view.accessibility.AccessibilityNodeInfo root = ASBLBridgeService.asblService.getRootInActiveWindow();
                if (root != null)
                {
                    root.refresh();
                    List<android.view.accessibility.AccessibilityNodeInfo> allNodes = new ArrayList<>();
                    HSQTools.getAllNodesRec(root, allNodes);

                    android.graphics.Point bestAsblPoint = null;
                    double bestAsblScore = Double.MAX_VALUE;

                    for (android.view.accessibility.AccessibilityNodeInfo node : allNodes)
                    {
                        String clazz = node.getClassName() != null ? node.getClassName().toString() : "";
                        boolean classLooksInput =
                                clazz.contains("Spinner")
                                        || clazz.contains("EditText")
                                        || clazz.contains("AutoComplete")
                                        || clazz.contains("Button");

                        CharSequence txt = node.getText();
                        if (txt == null || txt.toString().trim().isEmpty()) txt = node.getContentDescription();

                        String raw = txt == null ? "" : txt.toString().trim();
                        String rawCompact = raw.replaceAll("\\s+", "");
                        String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(raw));

                        String viewId = node.getViewIdResourceName();
                        android.graphics.Rect quickBounds = new android.graphics.Rect();
                        node.getBoundsInScreen(quickBounds);
                        boolean isNavButtonText = clazz.contains("Button") && isDropdownNavigationButtonText(clean);
                        boolean isHtmlSelectButton = isLikelyHtmlSelectButton(raw, viewId, clazz, quickBounds);
                        if (isNavButtonText && !isHtmlSelectButton) continue;


                        boolean sameTextAsLabel =
                                !clean.isEmpty()
                                        && (clean.equals(normTarget) || (clean.contains(normTarget) && clean.length() <= normTarget.length() + 2));

                        // 🔥 FAST-TRACK ASBL: Trúng Input xịn mới chốt!
                        if (sameTextAsLabel && classLooksInput) {
                            android.graphics.Rect bounds = new android.graphics.Rect();
                            node.getBoundsInScreen(bounds);
                            if (bounds.centerY() > 180 && bounds.centerY() < heightOfScreen - 20) {
                                // Lấy Center cho an toàn, tránh click sượt mép
                                bestAsblPoint = new android.graphics.Point(bounds.centerX(), bounds.centerY());
                                break;
                            }
                        }

                        if (!node.isClickable() && !node.isFocusable() && !classLooksInput) continue;

                        android.graphics.Rect bounds = new android.graphics.Rect();
                        node.getBoundsInScreen(bounds);

                        if (bounds.centerY() <= 180 || bounds.centerY() >= heightOfScreen - 20) continue;
                        if (bounds.width() < 80 || bounds.height() < 35 || bounds.height() > 320) continue;

                        boolean isClassicDropdownText =
                                clean.equals("v") || clean.equals("chon") || clean.equals("select") || clean.equals("choose")
                                        || clean.equals("chonmot") || clean.equals("selectone") || clean.contains("vuilongchon")
                                        || clean.equals("luachon") || clean.equals("vuilongluachon")
                                        || clean.equals("haychonmotphuongan") || clean.contains("pleaseselect")
                                        || raw.equals("-") || clean.equals("-")
                                        || raw.equals("...") || clean.equals("...");

                        boolean isMaskedDropdownPlaceholder = rawCompact.matches("^[=._\\-]{3,}$");

                        boolean belowLabel =
                                bounds.top >= exactTextNode.y - 10
                                        && bounds.top <= exactTextNode.y + 900
                                        && (Math.abs(bounds.centerX() - exactTextNode.x) < widthOfScreen || Math.abs(bounds.left - exactTextNode.x) < widthOfScreen);

                        boolean inlineRight =
                                Math.abs(bounds.centerY() - exactTextNode.y) <= 500
                                        && bounds.centerX() > exactTextNode.x + 120;

                        // 🌟 FIX ASBL
                        boolean surroundsText = bounds.contains(exactTextNode.x, exactTextNode.y);
                        boolean isSelf = sameTextAsLabel || surroundsText;

                        if (!belowLabel && !inlineRight && !isSelf) continue;

                        if (sameTextAsLabel && !isClassicDropdownText && !isMaskedDropdownPlaceholder && !node.isFocusable() && !classLooksInput && !node.isClickable())
                            continue;

                        double score = (Math.abs(bounds.top - exactTextNode.y) * 1.4) + (Math.abs(bounds.centerX() - exactTextNode.x) * 0.35);

                        if (inlineRight) score -= 120;
                        if (bounds.top > exactTextNode.y + 20) score -= 80;
                        if (isMaskedDropdownPlaceholder) score -= 650;
                        if (isClassicDropdownText) score -= 450;
                        if (node.isFocusable()) score -= 250;
                        if (classLooksInput) score -= 150;
                        if (isHtmlSelectButton) score -= 1800;

                        if (sameTextAsLabel) {
                            if (classLooksInput) score -= 2000;
                            else if (node.isClickable()) score -= 600;
                            else score += 900;
                        } else if (surroundsText) {
                            if (classLooksInput) score -= 1500;
                            else if (node.isClickable()) score -= 500;
                        }

                        if (bounds.width() > widthOfScreen - 40 && Math.abs(bounds.top - exactTextNode.y) > 300) continue;
                        if (bounds.centerY() <= exactTextNode.y + 60 && !inlineRight && !surroundsText && !sameTextAsLabel) score += 500;

                        if (score < bestAsblScore)
                        {
                            bestAsblScore = score;
                            int safeX = bounds.left + (int) (bounds.width() * 0.82);
                            if (safeX > bounds.right - 20) safeX = bounds.centerX();
                            bestAsblPoint = new android.graphics.Point(safeX, bounds.centerY());
                        }
                    }

                    root.recycle();

                    if (bestAsblPoint != null)
                    {
                        boolean existed = false;
                        for (android.graphics.Point oldPt : candidates)
                        {
                            if (Math.abs(oldPt.x - bestAsblPoint.x) < 60 && Math.abs(oldPt.y - bestAsblPoint.y) < 60)
                            {
                                existed = true;
                                break;
                            }
                        }

                        if (!existed)
                        {
                            candidates.add(bestAsblPoint);
                        }
                    }
                }
            }
            catch (Exception ignored) {}

            boolean isOpened = false;
            List<TextBlock> beforeClick = currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList());

            for (android.graphics.Point pt : candidates)
            {
                click(pt.x, pt.y, false);
                delay(4500);

                List<TextBlock> afterClick = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                boolean dropdownExpanded = isDropdownExpandedAfterClick(beforeClick, afterClick, 10);
                if (dropdownExpanded)
                {
                    updateNotificationContent("Dropdown mo: before=" + beforeClick.size() + ", after=" + afterClick.size());
                    isOpened = true;
                    break;
                }
            }

            if (isOpened) return null;

            if (vuotTimKiem == 0)
            {
                swipe(xCenter, yBot, xCenter, yTop, swipeDuration);
                delay(2000);

                List<TextBlock> afterSwipe = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                if (HSQTools.areAlmostSame(beforeClick, afterSwipe, 20))
                {
                    vuotTimKiem++;
                    swipe(xCenter, yTop, xCenter, yBot, swipeDuration);
                    delay(2000);
                }

                currentScreen = getCheckAnswerSmart();
            }
            else if (vuotTimKiem == 1)
            {
                swipe(xCenter, yTop, xCenter, yBot, swipeDuration);
                delay(2000);

                List<TextBlock> afterSwipe = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                if (HSQTools.areAlmostSame(beforeClick, afterSwipe, 20))
                {
                    return currentScreen;
                }

                currentScreen = getCheckAnswerSmart();
            }
        }
    }

    private boolean isDropdownExpandedAfterClick(List<TextBlock> beforeClick, List<TextBlock> afterClick, int tolerance)
    {
        List<TextBlock> before = getComparableDropdownBlocks(beforeClick);
        List<TextBlock> after = getComparableDropdownBlocks(afterClick);
        if (before.isEmpty() || after.size() < before.size() + 2) return false;

        int newBlockCount = 0;
        for (TextBlock afterBlock : after)
        {
            boolean existedBefore = false;
            for (TextBlock beforeBlock : before)
            {
                if (isComparableDropdownBlockSame(beforeBlock, afterBlock, tolerance))
                {
                    existedBefore = true;
                    break;
                }
            }

            if (!existedBefore)
            {
                newBlockCount++;
                if (newBlockCount >= 2) return true;
            }
        }

        return false;
    }

    private List<TextBlock> getComparableDropdownBlocks(List<TextBlock> source)
    {
        List<TextBlock> result = new ArrayList<>();
        if (source == null) return result;

        for (TextBlock block : source)
        {
            if (block == null || block.text == null) continue;
            String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(block.text));
            if (clean.isEmpty()) continue;
            if (clean.equals("davuotxuong") || clean.startsWith("blocked") || clean.startsWith("qqtruncated")) continue;
            if (clean.length() == 1 && !Character.isDigit(clean.charAt(0))) continue;
            if (clean.length() > 140) clean = clean.substring(0, 140);
            result.add(new TextBlock(clean, block.x, block.y));
        }

        return result;
    }

    private boolean isComparableDropdownBlockSame(TextBlock beforeBlock, TextBlock afterBlock, int tolerance)
    {
        if (beforeBlock == null || afterBlock == null) return false;
        boolean xNear = Math.abs(beforeBlock.x - afterBlock.x) <= tolerance + 18;
        boolean yNear = Math.abs(beforeBlock.y - afterBlock.y) <= tolerance + 55;
        if (!xNear || !yNear) return false;

        String beforeText = beforeBlock.text == null ? "" : beforeBlock.text;
        String afterText = afterBlock.text == null ? "" : afterBlock.text;
        if (beforeText.equals(afterText)) return true;

        int maxLen = Math.max(beforeText.length(), afterText.length());
        if (maxLen <= 2) return false;
        int dist = HSQTools.levenshtein(beforeText, afterText);
        if (maxLen <= 5) return dist <= 1;
        return dist <= Math.max(2, (int) (maxLen * 0.25));
    }
    private List<TextBlock> clickAccordion(String step)
    {
        Matcher match = java.util.regex.Pattern.compile("\\{([^}]+)\\}").matcher(step);
        if (!match.find()) return getCheckAnswerSmart(); // Lỗi cú pháp thì trả về màn hình hiện tại

        String innerContent = match.group(1);
        String[] sections = innerContent.split("\\^");
        List<TextBlock> currentScreen = getCheckAnswerSmart();

        for (String section : sections)
        {
            if (section == null || section.trim().isEmpty()) continue;
            String[] headerAndItems = section.split(":");
            if (headerAndItems.length != 2) continue;

            String headerStr = headerAndItems[0].trim();
            String[] itemsToClick = headerAndItems[1].split(",");

            updateNotificationContent("Smart Accordion: Tìm " + headerStr);

            List<HSQTools.TextBlock> tempCompareHeader = new ArrayList<>();
            int vuotTimKiemHeader = 0;

            while (true)
            {
                currentScreen = getCheckAnswerSmart();
                phoneScreen.addAll(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
                phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));
                List<TextBlock> visibleHeader = currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList());
                android.graphics.Point hPt = HSQTools.smartFindTextPoint(headerStr, heightOfScreen);

                // =======================================================
                // 1. TÌM VÀ ĐƯA HEADER LÊN CAO (SAFE ZONE)
                // =======================================================
                if (hPt == null)
                {
                    if (HSQTools.areAlmostSame(tempCompareHeader, visibleHeader, 20))
                    {
                        if (vuotTimKiemHeader == 0)
                        {
                            vuotTimKiemHeader = 1;
                            swipe(xCenter, yBot, xCenter, yTop, 2000);
                        }
                        else
                        {
                            return currentScreen; // Lỗi: Lật tung máy đéo thấy Header -> Trả về màn hình để handle lỗi
                        }
                    }
                    else
                    {
                        tempCompareHeader = new ArrayList<>(visibleHeader);
                        if (vuotTimKiemHeader == 0) swipe(xCenter, yBot, xCenter, yTop, 2000);
                        else swipe(xCenter, yTop, xCenter, yBot, 2000);
                    }
                    delay(2000);
                    continue;
                }

                if (hPt.y > 2300)
                {
                    int safeSwipeX = widthOfScreen / 2;
                    swipe(safeSwipeX, (int) (heightOfScreen * 0.8), safeSwipeX, (int) (heightOfScreen * 0.4), 2000);
                    delay(2500);
                    currentScreen = getCheckAnswerSmart();
                    phoneScreen.addAll(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
                    phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));
                    hPt = HSQTools.smartFindTextPoint(headerStr, heightOfScreen);
                    if (hPt == null)
                    {
                        swipe(safeSwipeX, (int) (heightOfScreen * 0.4), safeSwipeX, (int) (heightOfScreen * 0.8), 2000);
                        delay(2000);
                        continue;
                    }
                }

                // =======================================================
                // 2. 🚀 XÂY DỰNG "HÀNG RÀO THÉP" (DYNAMIC BOUNDARY)
                // =======================================================
                final int lockedHeaderY = hPt.y;
                int bottomBoundaryY = 2800; // Mặc định là đáy màn hình

                // Tìm Header tiếp theo (nếu có) để làm ranh giới đáy
                for (String otherSec : sections)
                {
                    if (otherSec.equals(section)) continue;
                    String otherHeader = otherSec.split(":")[0].trim();
                    String cleanOther = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(otherHeader));

                    HSQTools.TextBlock nextHeaderNode = currentScreen.stream()
                            .filter(x -> x.y > lockedHeaderY + 50 && HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text)).contains(cleanOther))
                            .min(Comparator.comparingInt(x -> x.y))
                            .orElse(null);

                    if (nextHeaderNode != null && nextHeaderNode.y < bottomBoundaryY)
                    {
                        bottomBoundaryY = nextHeaderNode.y; // Chốt hạ hàng rào!
                    }
                }

                // =======================================================
                // 3. KIỂM TRA ĐÃ MỞ CHƯA & MỞ THẺ (Dùng Hàng Rào)
                // =======================================================
                boolean isAlreadyOpen = false;
                int clickHeader = 0;

                while (!isAlreadyOpen)
                {
                    currentScreen = getCheckAnswerSmart();
                    phoneScreen.addAll(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
                    phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));

                    android.graphics.Point currentHeaderPt = HSQTools.smartFindTextPoint(headerStr, heightOfScreen);
                    if (currentHeaderPt == null) return currentScreen;

                    int currentBottomFence = 2800;
                    for (String otherSec : sections)
                    {
                        if (otherSec.equals(section)) continue;

                        String otherHeader = otherSec.split(":")[0].trim();
                        String cleanOther = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(otherHeader));

                        HSQTools.TextBlock nextHeaderNode = currentScreen.stream()
                                .filter(x -> x.y > currentHeaderPt.y + 50)
                                .filter(x -> HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text)).contains(cleanOther))
                                .min(Comparator.comparingInt(x -> x.y))
                                .orElse(null);

                        if (nextHeaderNode != null && nextHeaderNode.y < currentBottomFence)
                        {
                            currentBottomFence = nextHeaderNode.y;
                        }
                    }

                    String firstItem = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(itemsToClick[0].trim()));
                    final int headerYNow = currentHeaderPt.y;
                    final int fenceBottomNow = currentBottomFence;

                    List<String> textsInside = currentScreen.stream()
                            .filter(x -> x.y > headerYNow + 50 && x.y < fenceBottomNow)
                            .map(x -> HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text)))
                            .filter(x -> !x.isEmpty())
                            .collect(Collectors.toList());

                    boolean targetSeen = textsInside.stream().anyMatch(txt ->
                            txt.equals(firstItem) ||
                                    txt.contains(firstItem) ||
                                    firstItem.contains(txt) ||
                                    HSQTools.equalsOcrFriendly(txt, firstItem) ||
                                    HSQTools.containsOcrFriendly(txt, firstItem) ||
                                    HSQTools.levenshtein(txt, firstItem) <= Math.max(1, (int) (firstItem.length() * 0.25))
                    );

                    long visibleChoiceCount = textsInside.size();

                    isAlreadyOpen = targetSeen || visibleChoiceCount >= 2;

                    if (!isAlreadyOpen)
                    {
                        if (clickHeader > 2) return currentScreen;

                        updateNotificationContent("Đang mở thẻ Accordion...");
                        click(currentHeaderPt.x, currentHeaderPt.y, false);
                        delay(2500);

                        if (clickHeader == 1)
                        {
                            click((int) (widthOfScreen * 0.9), currentHeaderPt.y, false);
                            delay(2000);
                        }

                        clickHeader++;
                    }
                }

                // =======================================================
                // 4. CLICK ITEMS BÊN TRONG (Ép xài Hàng Rào Thép)
                // =======================================================
                for (String item : itemsToClick)
                {
                    String cleanItem = item.trim();
                    String targetStr = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(cleanItem));
                    final String cleanHeader = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(headerStr));

                    int vuotTimKiemItem = 0;
                    List<HSQTools.TextBlock> tempCompareItem = new ArrayList<>();

                    while (true)
                    {
                        currentScreen = getCheckAnswerSmart();
                        phoneScreen.addAll(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
                        phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));
                        android.graphics.Point currentHeaderPt = HSQTools.smartFindTextPoint(headerStr, heightOfScreen);

                        if (currentHeaderPt == null)
                        {
                            swipe(widthOfScreen / 2, (int) (heightOfScreen * 0.4), widthOfScreen / 2, (int) (heightOfScreen * 0.7), 2000);
                            delay(2000);
                            continue;
                        }

                        // Tái tính toán lại Hàng rào (vì vuốt xong tọa độ thay đổi)
                        int currentBottomFence = 2800;
                        for (String otherSec : sections)
                        {
                            if (otherSec.equals(section)) continue;
                            String otherHeader = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(otherSec.split(":")[0]));
                            HSQTools.TextBlock nextHNode = currentScreen.stream()
                                    .filter(x -> x.y > currentHeaderPt.y + 50 && HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text)).contains(otherHeader))
                                    .min(Comparator.comparingInt(x -> x.y)).orElse(null);
                            if (nextHNode != null && nextHNode.y < currentBottomFence)
                                currentBottomFence = nextHNode.y;
                        }

                        final int currentFence = currentBottomFence;
                        final int curHeaderY = currentHeaderPt.y;

                        // Sử dụng VŨ KHÍ TỐI THƯỢNG đã được bọc thép tọa độ Hàng Rào!
                        android.graphics.Point exactPt = HSQTools.smartFindTextPoint(targetStr, heightOfScreen, curHeaderY + 50, currentFence);

                        if (exactPt != null)
                        {
                            updateNotificationContent("Đã khóa mục tiêu [" + cleanItem + "] tại Y=" + exactPt.y);
                            click(exactPt.x, exactPt.y, false);
                            delay(2000);
                            break;
                        }

                        // Vì smartFindTextPoint trả về tọa độ nên ta cần một biến tương đương để check Fallback bên dưới
                        HSQTools.TextBlock exactItemNode = null; 

                        // =======================================================
                        // 🌟 FALLBACK: NỘI SUY THANG LIKERT (1-7, 1-10, 1-5...)
                        // Khi OCR mù số đơn lẻ, dùng 2 mốc neo đầu-cuối để tính
                        // =======================================================
                        if (exactItemNode == null && targetStr.matches("\\d+"))
                        {
                            int targetNum = Integer.parseInt(targetStr);

                            // Tìm 2 mốc neo: text có chứa số ở đầu hoặc cuối
                            int anchorMinNum = -1, anchorMaxNum = -1;
                            int anchorMinY = -1, anchorMaxY = -1;

                            for (TextBlock block : currentScreen)
                            {
                                if (block.y <= curHeaderY + 30 || block.y >= currentFence) continue;

                                String blockText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(block.text));
                                if (blockText.isEmpty()) continue;

                                // Tìm số ở CUỐI chuỗi (VD: "khongmotatot1" → 1)
                                java.util.regex.Matcher numMatcher = java.util.regex.Pattern.compile("(\\d+)$").matcher(blockText);
                                if (!numMatcher.find())
                                {
                                    // Thử tìm số ở ĐẦU chuỗi (VD: "1khongmotatot")
                                    numMatcher = java.util.regex.Pattern.compile("^(\\d+)").matcher(blockText);
                                    if (!numMatcher.find()) continue;
                                }

                                int foundNum = Integer.parseInt(numMatcher.group(1));
                                if (foundNum < 0 || foundNum > 20) continue; // Bỏ qua số vô nghĩa

                                if (anchorMinNum == -1 || foundNum < anchorMinNum)
                                {
                                    anchorMinNum = foundNum;
                                    anchorMinY = block.y;
                                }
                                if (anchorMaxNum == -1 || foundNum > anchorMaxNum)
                                {
                                    anchorMaxNum = foundNum;
                                    anchorMaxY = block.y;
                                }
                            }
                            // =======================================================
                            // 🌟 FALLBACK: KHI OCR MÙ SỐ ĐƠN LẺ (LIKERT SCALE)
                            // Tầng 1: XML RadioButton → Tầng 2: Nội suy toán học
                            // =======================================================
                            if (exactItemNode == null && targetStr.matches("\\d+"))
                            {
                                targetNum = Integer.parseInt(targetStr);
                                boolean clicked = false;

                                // === TẦNG 1: XML RADIOBUTTON ===
                                try
                                {
                                    String xml = HSQTools.getFlexibleXML();
                                    javax.xml.parsers.DocumentBuilder xmlBuilder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
                                    org.w3c.dom.Document doc = xmlBuilder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                    org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                    int xmlYMin = curHeaderY + 30;
                                    int xmlYMax = currentFence - 30;
                                    List<android.graphics.Rect> validRadios = new ArrayList<>();

                                    for (int i = 0; i < nodes.getLength(); i++)
                                    {
                                        org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                        if (r == null) continue;
                                        if (r.width() <= 0 || r.height() <= 0) continue;
                                        if (r.width() < 15 || r.height() < 15) continue;
                                        if (r.width() > 200 || r.height() > 200) continue;
                                        float ratio = (float) Math.max(r.width(), r.height()) / Math.min(r.width(), r.height());
                                        if (ratio > 2.5f) continue;
                                        if (r.centerY() < xmlYMin || r.centerY() > xmlYMax) continue;
                                        if (r.centerX() < 0 || r.centerX() > widthOfScreen) continue;

                                        String className = node.getAttribute("class").toLowerCase();
                                        boolean isCheckable = "true".equals(node.getAttribute("checkable"));
                                        boolean isClickable = "true".equals(node.getAttribute("clickable"));
                                        boolean isRadioClass = className.contains("radio") || className.contains("checkbox");
                                        boolean hasNoText = node.getAttribute("text").trim().isEmpty() && node.getAttribute("content-desc").trim().isEmpty();

                                        boolean isValidRadio = isRadioClass ||
                                                (isCheckable && isClickable) ||
                                                (isClickable && hasNoText && r.width() >= 20 && r.width() <= 150);
                                        if (!isValidRadio) continue;

                                        boolean isDup = false;
                                        for (android.graphics.Rect existing : validRadios)
                                        {
                                            if (Math.abs(existing.centerY() - r.centerY()) < 30) { isDup = true; break; }
                                        }
                                        if (!isDup) validRadios.add(r);
                                    }

                                    validRadios.sort(Comparator.comparingInt(android.graphics.Rect::centerY));

                                    int targetIndex = targetNum - 1;
                                    if (targetIndex >= 0 && targetIndex < validRadios.size())
                                    {
                                        android.graphics.Rect targetRadio = validRadios.get(targetIndex);
                                        updateNotificationContent("XML Radio: " + validRadios.size() + " lỗ. Click #" + targetNum + " tại X=" + targetRadio.centerX() + ", Y=" + targetRadio.centerY());
                                        click(targetRadio.centerX(), targetRadio.centerY(), false);
                                        delay(2000);
                                        clicked = true;
                                    }
                                }
                                catch (Exception ignored) { }

                                // === TẦNG 2: NỘI SUY LIKERT (Chỉ chạy nếu XML thất bại) ===
                                if (!clicked)
                                {
                                    anchorMinNum = -1; anchorMaxNum = -1;
                                    anchorMinY = -1; anchorMaxY = -1;

                                    for (TextBlock block : currentScreen)
                                    {
                                        if (block.y <= curHeaderY + 30 || block.y >= currentFence) continue;
                                        String blockText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(block.text));
                                        if (blockText.isEmpty()) continue;

                                        java.util.regex.Matcher numMatcher = java.util.regex.Pattern.compile("(\\d+)$").matcher(blockText);
                                        if (!numMatcher.find())
                                        {
                                            numMatcher = java.util.regex.Pattern.compile("^(\\d+)").matcher(blockText);
                                            if (!numMatcher.find()) continue;
                                        }
                                        int foundNum = Integer.parseInt(numMatcher.group(1));
                                        if (foundNum < 0 || foundNum > 20) continue;

                                        if (anchorMinNum == -1 || foundNum < anchorMinNum) { anchorMinNum = foundNum; anchorMinY = block.y; }
                                        if (anchorMaxNum == -1 || foundNum > anchorMaxNum) { anchorMaxNum = foundNum; anchorMaxY = block.y; }
                                    }

                                    if (anchorMinNum != -1 && anchorMaxNum != -1 && anchorMinNum != anchorMaxNum
                                            && targetNum >= anchorMinNum && targetNum <= anchorMaxNum)
                                    {
                                        double likertStep = (double)(anchorMaxY - anchorMinY) / (anchorMaxNum - anchorMinNum);
                                        int interpolatedY = (int)(anchorMinY + (targetNum - anchorMinNum) * likertStep);

                                        int clickX = (int)(widthOfScreen * 0.85);
                                        for (TextBlock block : currentScreen)
                                        {
                                            if (block.y > curHeaderY + 30 && block.y < currentFence)
                                            {
                                                String raw = block.text.replaceAll("[^oO0\\-\\s]", "");
                                                if (raw.length() >= 3 && block.x > widthOfScreen * 0.5) { clickX = block.x; break; }
                                            }
                                        }

                                        updateNotificationContent("Likert: [" + anchorMinNum + "→Y=" + anchorMinY + "] đến [" + anchorMaxNum + "→Y=" + anchorMaxY + "]. Click #" + targetNum + " tại Y=" + interpolatedY);
                                        click(clickX, interpolatedY, false);
                                        delay(2000);
                                        clicked = true;
                                    }
                                }

                                if (clicked) break;
                            }

                            // =======================================================
                            // 🌟 FALLBACK TẦNG 2: NỘI SUY THANG LIKERT (TOÁN HỌC)
                            // Khi cả OCR lẫn XML đều bó tay, dùng 2 mốc neo đầu-cuối để tính
                            // =======================================================
                            if (exactItemNode == null && targetStr.matches("\\d+"))
                            {
                                targetNum = Integer.parseInt(targetStr);
                                anchorMinNum = -1; anchorMaxNum = -1;
                                anchorMinY = -1; anchorMaxY = -1;

                                for (TextBlock block : currentScreen)
                                {
                                    if (block.y <= curHeaderY + 30 || block.y >= currentFence) continue;
                                    String blockText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(block.text));
                                    if (blockText.isEmpty()) continue;

                                    // Tìm số ở CUỐI chuỗi (VD: "khongmotatot1" → 1)
                                    java.util.regex.Matcher numMatcher = java.util.regex.Pattern.compile("(\\d+)$").matcher(blockText);
                                    if (!numMatcher.find())
                                    {
                                        // Thử tìm số ở ĐẦU chuỗi
                                        numMatcher = java.util.regex.Pattern.compile("^(\\d+)").matcher(blockText);
                                        if (!numMatcher.find()) continue;
                                    }

                                    int foundNum = Integer.parseInt(numMatcher.group(1));
                                    if (foundNum < 0 || foundNum > 20) continue;

                                    if (anchorMinNum == -1 || foundNum < anchorMinNum)
                                    {
                                        anchorMinNum = foundNum;
                                        anchorMinY = block.y;
                                    }
                                    if (anchorMaxNum == -1 || foundNum > anchorMaxNum)
                                    {
                                        anchorMaxNum = foundNum;
                                        anchorMaxY = block.y;
                                    }
                                }

                                if (anchorMinNum != -1 && anchorMaxNum != -1 && anchorMinNum != anchorMaxNum
                                        && targetNum >= anchorMinNum && targetNum <= anchorMaxNum)
                                {
                                    double stepY = (double)(anchorMaxY - anchorMinY) / (anchorMaxNum - anchorMinNum);
                                    int interpolatedY = (int)(anchorMinY + (targetNum - anchorMinNum) * stepY);

                                    // X: ưu tiên dùng X của chuỗi radio "O-O-OO..." nếu có
                                    int clickX = (int)(widthOfScreen * 0.85);
                                    for (TextBlock block : currentScreen)
                                    {
                                        if (block.y > curHeaderY + 30 && block.y < currentFence)
                                        {
                                            String raw = block.text.replaceAll("[^oO0\\-\\s]", "");
                                            if (raw.length() >= 3 && block.x > widthOfScreen * 0.5)
                                            {
                                                clickX = block.x;
                                                break;
                                            }
                                        }
                                    }

                                    updateNotificationContent("Likert Interpolate: [" + anchorMinNum + "→Y=" + anchorMinY
                                            + "] đến [" + anchorMaxNum + "→Y=" + anchorMaxY + "]. Click #" + targetNum + " tại Y=" + interpolatedY);
                                    click(clickX, interpolatedY, false);
                                    delay(2000);
                                    break;
                                }
                            }
                            // Cần ít nhất 2 mốc neo KHÁC NHAU để nội suy
                            if (anchorMinNum != -1 && anchorMaxNum != -1 && anchorMinNum != anchorMaxNum
                                    && targetNum >= anchorMinNum && targetNum <= anchorMaxNum)
                            {
                                // Nội suy tuyến tính
                                double stepY = (double)(anchorMaxY - anchorMinY) / (anchorMaxNum - anchorMinNum);
                                int interpolatedY = (int)(anchorMinY + (targetNum - anchorMinNum) * stepY);

                                // X: click bên phải (vào vùng radio), dùng X của block "O-O-OO..." nếu có,
                                // hoặc mặc định vùng phải màn hình
                                int clickX = (int)(widthOfScreen * 0.85);
                                for (TextBlock block : currentScreen)
                                {
                                    if (block.y > curHeaderY + 30 && block.y < currentFence)
                                    {
                                        String txt = block.text.replaceAll("[^oO0\\-]", "");
                                        if (txt.length() >= 3 && block.x > widthOfScreen * 0.5)
                                        {
                                            clickX = block.x; // Dùng X của chuỗi radio
                                            break;
                                        }
                                    }
                                }

                                updateNotificationContent("Likert Interpolate: Mốc [" + anchorMinNum + "→Y=" + anchorMinY
                                        + "] đến [" + anchorMaxNum + "→Y=" + anchorMaxY + "]. Click " + targetNum + " tại Y=" + interpolatedY);
                                click(clickX, interpolatedY, false);
                                delay(2000);
                                break;
                            }
                        }

                        if (HSQTools.areAlmostSame(tempCompareItem, currentScreen, 20))
                        {
                            if (vuotTimKiemItem == 0)
                            {
                                vuotTimKiemItem = 1;
                                swipe(xs, yBot, xs, yTop, 2000);
                            }
                            else
                            {
                                return currentScreen; // Lỗi đéo thấy Item -> Trả màn hình về
                            }
                        }
                        else
                        {
                            tempCompareItem = new ArrayList<>(currentScreen);
                            if (vuotTimKiemItem == 0)
                                swipe(xs, yBot, xs, yTop, 2000);
                            else swipe(xs, yTop, xs, yBot, 2000);
                        }
                        delay(2000);
                    }
                }
                break; // Xong Section này, văng ra ngoài để làm Section tiếp theo
            }
        }
        return null; // THÀNH CÔNG RỰC RỠ!
    }

    private int getDomDistance(org.w3c.dom.Node a, org.w3c.dom.Node b) {
        if (a == null || b == null) return 999;
        java.util.List<org.w3c.dom.Node> aParents = new java.util.ArrayList<>();
        org.w3c.dom.Node curr = a;
        while (curr != null) {
            aParents.add(curr);
            curr = curr.getParentNode();
        }
        
        curr = b;
        int bDepth = 0;
        while (curr != null) {
            int aIndex = aParents.indexOf(curr);
            if (aIndex != -1) return aIndex + bDepth;
            bDepth++;
            curr = curr.getParentNode();
        }
        return 999;
    }

    private List<HSQTools.TextBlock> clickInput(String labelToFind, String valueToInput)
    {
        List<HSQTools.TextBlock> tempCompare = new ArrayList<>();
        int vuotLenLai = 0; // 0: Đang cuộn xuống tìm, 1: Đang cuộn ngược lên tìm

        while (true)
        {
            List<HSQTools.TextBlock> currentScreen = getCheckAnswerSmart();
            phoneScreen.addAll(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
            phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));
            int inputX = -1, inputY = -1;

            // =========================================================
            // 🎯 1. TẦNG BỌC THÉP XML: QUÉT 2 LƯỢT & CHỐNG NÚT ẢO
            // =========================================================
            String xml = HSQTools.getFlexibleXML();
            try
            {
                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                String normTarget = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(labelToFind));

                // 🚀 LƯỢT 1: SIÊU ƯU TIÊN PLACEHOLDER (EditText chứa thẳng chữ)
                for (int i = 0; i < nodes.getLength(); i++)
                {
                    org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                    if (node.getAttribute("class").contains("EditText") || node.getAttribute("class").contains("AutoCompleteTextView"))
                    {
                        String combined = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(node.getAttribute("text") + " " + node.getAttribute("content-desc")));
                        if (!combined.isEmpty() && (combined.equals(normTarget) || combined.contains(normTarget)))
                        {
                            android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                            // 🔥 BỌC THÉP HEIGHT > 10: Diệt tận gốc các thẻ EditText bị WebView ép dẹp lép (Ghost node)
                            if (r != null && r.height() > 10 && r.centerY() > 180 && r.centerY() < heightOfScreen - 100)
                            {
                                inputX = r.centerX();
                                inputY = r.centerY();
                                updateNotificationContent("XML: Tóm gọn Placeholder VIP tại Y=" + inputY);
                                break;
                            }
                        }
                    }
                }

                // 🚀 LƯỢT 2: TÌM LABEL CHUẨN -> TÌM EDITTEXT NẰM DƯỚI (SIBLING)
                if (inputX == -1 && inputY == -1)
                {
                    int labelBottom = -1;
                    int minLabelHeight = 99999;
                    org.w3c.dom.Element bestLabelNode = null;

                    // 2.1: Tìm Mỏ Neo (Label)
                    for (int i = 0; i < nodes.getLength(); i++)
                    {
                        org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                        String clazz = node.getAttribute("class");

                        if (!clazz.contains("EditText") && !clazz.contains("Button") && !clazz.contains("WebView"))
                        {
                            String combined = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(node.getAttribute("text") + " " + node.getAttribute("content-desc")));
                            android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                            // 🔥 BỌC THÉP HEIGHT > 10: Diệt các mỏ neo ảo bị ghim trên mép màn hình
                            if (r != null && !combined.isEmpty() && r.height() > 10 && r.height() < (heightOfScreen * 0.8) && normTarget.length() >= 2)
                            {
                                if (r.centerY() > 100 && r.centerY() < heightOfScreen - 50)
                                {
                                    if (combined.equals(normTarget) || combined.contains(normTarget))
                                    {
                                        if (r.height() < minLabelHeight)
                                        {
                                            minLabelHeight = r.height();
                                            labelBottom = r.centerY(); // Thực chất đây là CenterY, dùng biến cũ để đỡ phải khai báo lại
                                            bestLabelNode = node;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2.2: Dò mìn tìm EditText GẦN MỎ NEO NHẤT
                    if (labelBottom != -1)
                    {
                        int minXmlDist = 99999;
                        android.graphics.Rect bestXmlInput = null;

                        for (int i = 0; i < nodes.getLength(); i++)
                        {
                            org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                            if (node.getAttribute("class").contains("EditText") || node.getAttribute("class").contains("AutoCompleteTextView"))
                            {
                                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                                // 🔥 BỌC THÉP HEIGHT > 10 LẦN NỮA CHO CHẮC CÚ
                                if (r != null && r.height() > 10)
                                {
                                    int dist = Math.abs(r.centerY() - labelBottom);
                                    int domDist = getDomDistance(node, bestLabelNode);
                                    int score = dist + (domDist * 50);

                                    if (score < minXmlDist && dist < (heightOfScreen * 0.45))
                                    {
                                        minXmlDist = score;
                                        bestXmlInput = r;
                                    }
                                }
                            }
                        }
                        
                        if (bestXmlInput != null && bestXmlInput.centerY() > 180 && bestXmlInput.centerY() < heightOfScreen - 100)
                        {
                            inputX = bestXmlInput.centerX();
                            inputY = bestXmlInput.centerY();
                        }
                    }
                }
            }
            catch (Exception ignored)
            {
            }
            if (inputX == -1 && inputY == -1)
            {
                android.graphics.Rect looseXmlInput = findInputRectByXmlLoose(xml, labelToFind);
                if (looseXmlInput != null)
                {
                    inputX = looseXmlInput.centerX();
                    inputY = looseXmlInput.centerY();
                    updateNotificationContent("XML loose: Tóm được ô nhập tại X=" + inputX + ", Y=" + inputY);
                }
            }
            // =========================================================
            // 🎯 2. FALLBACK: DÙNG ASBL (CŨNG BỌC THÉP NÚT ẢO LUÔN)
            // =========================================================
            if (inputX == -1 && inputY == -1)
            {
                android.graphics.Point tableGeomPt = findRightSideTableInputPointForLabel(currentScreen, labelToFind);
                if (tableGeomPt != null)
                {
                    inputX = tableGeomPt.x;
                    inputY = tableGeomPt.y;
                }
            }
            if (inputX == -1 && inputY == -1)
            {
                android.graphics.Point labelPt = HSQTools.smartFindTextPoint(labelToFind, heightOfScreen);
                if (labelPt != null && labelPt.y > 180 && labelPt.y < heightOfScreen - 100) // Label phải nhìn thấy
                {
                    AccessibilityNodeInfo root = ASBLBridgeService.asblService.getRootInActiveWindow();
                    android.graphics.Rect inputRect = findInputNearYByASBL(root, labelPt.y);
                    if (inputRect != null && inputRect.centerY() > 180 && inputRect.centerY() < heightOfScreen - 100)
                    {
                        inputX = inputRect.centerX();
                        inputY = inputRect.centerY();
                        updateNotificationContent("ASBL: Tóm được ô nhập tại Y=" + inputY);
                    }
                    if (root != null) root.recycle();
                }
            }

            // =========================================================
            // 🎯 2.5 FALLBACK TỐI THƯỢNG: VISUAL GEOMETRY (MẮT THẦN HÌNH HỌC)
            // Nếu XML và ASBL cũ đều đui mù, xách Mắt thần ra dò theo tọa độ Y!
            // =========================================================
            if (inputX == -1 && inputY == -1)
            {
                android.graphics.Point geomPt = findInputByVisualGeometry(labelToFind);
                if (geomPt != null)
                {
                    inputX = geomPt.x;
                    inputY = geomPt.y;
                }
            }
            // =========================================================
            // 🎯 3. THỰC THI CLICK VÀ NHẬP TEXT
            // =========================================================
             if (inputX != -1 && inputY != -1)
            {
                updateNotificationContent("Chọt ô nhập: X=" + inputX + ", Y=" + inputY);
                click(inputX, inputY, false);
                delay(3000);

                if (HSQTools.isKeyboardVisibleSmart())
                {
                    clearAllText();
                    delay(1000);
                    inputText(valueToInput, null, true);
                    delay(2500);
                    globalBack();
                    delay(2000);
                    return null; // THÀNH CÔNG RỰC RỠ!
                }
                else
                {
                    // Bàn phím đéo lên -> Báo lỗi cho vòng lặp ngoài xử lý
                    return currentScreen;
                }
            }

            // =========================================================
            // 🎯 4. THUẬT TOÁN VUỐT XÓC ĐĨA (VÌ NÚT ẢO BỊ ĐÁ VĂNG NÊN NÓ SẼ CHẠY XUỐNG ĐÂY)
            // =========================================================
            List<HSQTools.TextBlock> currentVisible = currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList());
            if (HSQTools.areAlmostSame(tempCompare, currentVisible, 20))
            {
                if (vuotLenLai == 0)
                {
                    vuotLenLai++;
                    updateNotificationContent("Chạm đáy! Quay xe cuộn lên...");
                }
                else
                {
                    return currentScreen; // LỖI: Cuộn lật tung máy đéo thấy
                }
            }

            tempCompare = new ArrayList<>(currentVisible);
            if (vuotLenLai == 0) smartScroll(ysBot, ysTop, "input"); // Vuốt xuống
            else smartScroll(ysTop, ysBot, "input"); // Vuốt lên
            delay(2500); // Chờ WebView ổn định
        }
    }

    private boolean checkNextOK(List<TextBlock> beginClick, String step)
    {
        int checkAgain = 0;
        while (true)
        {
            delay(10000);
            List<HSQTools.TextBlock> afterClick = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
            if (HSQTools.areAlmostSame(afterClick, beginClick, 5))
            {
                if (checkAgain < 6)
                {
                    checkAgain++;
                }
                else
                {
                    if(tryNextAgain > 0)
                    {
                        String currentXml = HSQTools.getFlexibleXML();
                        HSQFileHelper.deleteFile(imagePath);
                        HSQFileHelper.createFolder(imagePath);
                        delay(1000);
                        HSQTools.captureAndSaveScreen(imagePath + "/screenCap.png");
                        delay(2000);
                        HSQTools.ScanImage(imagePath);
                        // CHUYỂN ĐỔI PHONESCREEN SANG MẢNG JSON
                        org.json.JSONArray jsonArray = new org.json.JSONArray();
                        org.json.JSONObject currentScreen = new org.json.JSONObject();
                        org.json.JSONArray currentBlocks = new org.json.JSONArray();
                        int screenIndex = 1;
                        for (HSQTools.TextBlock tb : phoneScreen) {
                            if (tb.text.equals("Đã vuốt xuống")) {
                                try {
                                    currentScreen.put("screen_index", screenIndex++);
                                    currentScreen.put("blocks", currentBlocks);
                                    jsonArray.put(currentScreen);
                                } catch(Exception ignored){}
                                currentScreen = new org.json.JSONObject();
                                currentBlocks = new org.json.JSONArray();
                            } else {
                                try {
                                    org.json.JSONObject b = new org.json.JSONObject();
                                    b.put("text", tb.text);
                                    b.put("x", tb.x);
                                    b.put("y", tb.y);
                                    currentBlocks.put(b);
                                } catch(Exception ignored){}
                            }
                        }
                        String jsonContent = jsonArray.toString();
                        HSQTools.sendTelegramAlertVIP(deviceID, "Bấm next không ăn " + step, idTelegram, imagePath + "/screenCap.png", currentXml, jsonContent);
                        updateNotificationContent("next không ăn");
                        List<TextBlock> beginSend = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                        while (true)
                        {
                            delay(180000);
                            List<HSQTools.TextBlock> afterSend = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                            if (!HSQTools.areAlmostSame(beginSend, afterSend, 20))
                            {
                                tryNextAgain = 0;
                                delay(2000);
                                return true;
                            }
                        }
                    }
                    tryNextAgain++;
                    return false;
                }
            }
            else
            {
                tryNextAgain = 0;
                return true;
            }
        }
    }

    private android.graphics.Point findInputByVisualGeometry(String anchorText) {
        List<HSQTools.TextBlock> screen = getCheckAnswerSmart();
        String normAnchor = cleanInputGeometryText(anchorText);
        if (normAnchor.length() < 3) return null;

        HSQTools.TextBlock labelNode = null;
        int bestLabelScore = Integer.MAX_VALUE;
        for (HSQTools.TextBlock node : screen) {
            if (node == null || node.y <= 180) continue;
            int score = getInputAnchorMatchScore(node.text, normAnchor);
            if (score < bestLabelScore) {
                bestLabelScore = score;
                labelNode = node;
            }
        }

        if (labelNode == null || bestLabelScore == Integer.MAX_VALUE) return null;

        android.graphics.Point tablePoint = findRightSideTableInputPoint(screen, labelNode);
        if (tablePoint != null) {
            return tablePoint;
        }

        AccessibilityNodeInfo root = null;
        try {
            if (ASBLBridgeService.asblService != null) {
                root = ASBLBridgeService.asblService.getRootInActiveWindow();
            }
        } catch (Exception ignored) {}

        if (root != null) {
            java.util.List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
            HSQTools.getAllNodesRec(root, allNodes);

            android.graphics.Rect bestBox = null;
            int minDistance = 99999;

            for (AccessibilityNodeInfo node : allNodes) {
                if (node == null) continue;
                String clazz = node.getClassName() != null ? node.getClassName().toString() : "";
                boolean isEditText = clazz.contains("EditText") || clazz.contains("AutoCompleteTextView");
                boolean isEmptyClickable = node.isClickable() && (node.getText() == null || node.getText().toString().trim().isEmpty());

                if (isEditText || isEmptyClickable) {
                    android.graphics.Rect r = new android.graphics.Rect();
                    node.getBoundsInScreen(r);

                    if (r.centerY() > 180 && r.centerY() < heightOfScreen - 100) {
                        int dist = r.top - labelNode.y;
                        if (dist >= -30 && dist < minDistance && dist < 600) {
                            minDistance = dist;
                            bestBox = r;
                        }
                    }
                }
            }
            root.recycle();

            if (bestBox != null) {
                updateNotificationContent("Visual Geometry: Chot o cach mo neo " + minDistance + "px tai Y=" + bestBox.centerY());
                return new android.graphics.Point(bestBox.centerX(), bestBox.centerY());
            }
        }
        java.util.List<HSQTools.TextBlock> sortedBlocks = new java.util.ArrayList<>(screen);
        sortedBlocks.sort(Comparator.comparingInt(t -> t.y));

        java.util.List<HSQTools.TextBlock> blocksBelow = new java.util.ArrayList<>();
        for (HSQTools.TextBlock node : sortedBlocks) {
            if (node.y >= labelNode.y) {
                blocksBelow.add(node);
            }
        }

        int targetY = -1;
        for (int i = 0; i < blocksBelow.size() - 1; i++) {
            HSQTools.TextBlock current = blocksBelow.get(i);
            HSQTools.TextBlock next = blocksBelow.get(i + 1);
            int gap = next.y - current.y;

            if (gap > 200 && (current.y - labelNode.y) < 800) {
                targetY = current.y + 100;
                break;
            }
        }

        if (targetY == -1) {
            int bottomOfTextY = labelNode.y;
            for (HSQTools.TextBlock node : blocksBelow) {
                if (node.y > bottomOfTextY && node.y < bottomOfTextY + 200) {
                    bottomOfTextY = node.y;
                } else if (node.y >= bottomOfTextY + 200) {
                    break;
                }
            }
            targetY = bottomOfTextY + 100;
        }

        int blindClickX = labelNode.x + 100;
        int blindClickY = targetY;

        if (blindClickY > 180 && blindClickY < heightOfScreen - 100) {
            updateNotificationContent("WebView Blind Fallback: Bam mu duoi mo neo Y=" + blindClickY);
            return new android.graphics.Point(blindClickX, blindClickY);
        }

        return null;
    }

    private android.graphics.Point findRightSideTableInputPointForLabel(List<HSQTools.TextBlock> screen, String anchorText) {
        if (screen == null) return null;
        String normAnchor = cleanInputGeometryText(anchorText);
        if (normAnchor.length() < 3) return null;

        HSQTools.TextBlock labelNode = null;
        int bestLabelScore = Integer.MAX_VALUE;
        for (HSQTools.TextBlock node : screen) {
            if (node == null || node.y <= 180) continue;
            int score = getInputAnchorMatchScore(node.text, normAnchor);
            if (score < bestLabelScore) {
                bestLabelScore = score;
                labelNode = node;
            }
        }

        if (labelNode == null || bestLabelScore == Integer.MAX_VALUE) return null;
        return findRightSideTableInputPoint(screen, labelNode);
    }
    private android.graphics.Point findRightSideTableInputPoint(List<HSQTools.TextBlock> screen, HSQTools.TextBlock labelNode) {
        if (screen == null || labelNode == null) return null;
        if (labelNode.x > widthOfScreen * 0.62f) return null;

        int minX = Math.max(labelNode.x + 180, (int) (widthOfScreen * 0.55f));
        HSQTools.TextBlock bestUnit = null;
        int bestScore = Integer.MAX_VALUE;

        for (HSQTools.TextBlock node : screen) {
            if (node == null || node == labelNode) continue;
            if (node.y <= 180 || node.y >= heightOfScreen - 80) continue;
            if (node.x < minX) continue;
            if (!isLikelyInputUnitBlock(node.text)) continue;

            int dy = node.y - labelNode.y;
            if (dy < -40 || dy > 280) continue;

            int score = Math.abs(dy - 115) + Math.abs(node.x - (int) (widthOfScreen * 0.78f)) / 6;
            if (score < bestScore) {
                bestScore = score;
                bestUnit = node;
            }
        }

        if (bestUnit == null) return null;

        int clickX = Math.max(minX, Math.min(widthOfScreen - 80, bestUnit.x));
        int clickY = bestUnit.y - 125;
        if (clickY < labelNode.y - 90 || clickY > labelNode.y + 75) {
            clickY = labelNode.y - 15;
        }
        clickY = Math.max(181, Math.min(heightOfScreen - 120, clickY));

        updateNotificationContent("Visual Table Input: bam o ben phai label tai X=" + clickX + ", Y=" + clickY);
        return new android.graphics.Point(clickX, clickY);
    }

    private boolean isLikelyInputUnitBlock(String rawText) {
        for (String clean : getInputCleanVariants(rawText)) {
            if (clean.contains("000") && (clean.contains("vnd") || clean.contains("vn") || clean.contains("dong") || clean.endsWith("d"))) {
                return true;
            }
        }
        return false;
    }

    private int getInputAnchorMatchScore(String rawText, String normAnchor) {
        if (normAnchor == null || normAnchor.length() < 3) return Integer.MAX_VALUE;
        int best = Integer.MAX_VALUE;
        for (String clean : getInputCleanVariants(rawText)) {
            if (clean.length() < 3) continue;
            if (clean.equals(normAnchor)) best = Math.min(best, 0);
            if (HSQTools.containsOcrFriendly(clean, normAnchor) || HSQTools.containsOcrFriendly(normAnchor, clean)) {
                best = Math.min(best, 5 + Math.abs(clean.length() - normAnchor.length()));
            }
            if (clean.length() >= 5 && Math.abs(clean.length() - normAnchor.length()) <= Math.max(4, normAnchor.length() / 3)) {
                int dist = HSQTools.levenshtein(clean, normAnchor);
                int allowed = Math.max(2, (int) (normAnchor.length() * 0.22f));
                if (dist <= allowed) {
                    best = Math.min(best, 20 + dist);
                }
            }
        }
        return best;
    }

    private String cleanInputGeometryText(String rawText) {
        java.util.List<String> variants = getInputCleanVariants(rawText);
        return variants.isEmpty() ? "" : variants.get(0);
    }

    private java.util.List<String> getInputCleanVariants(String rawText) {
        java.util.List<String> result = new java.util.ArrayList<>();
        addInputCleanVariant(result, rawText);
        addInputCleanVariant(result, repairUtf8Mojibake(rawText));
        return result;
    }

    private void addInputCleanVariant(java.util.List<String> result, String rawText) {
        if (rawText == null) return;
        String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawText));
        if (clean.isEmpty() || result.contains(clean)) return;
        result.add(clean);
    }

    private String repairUtf8Mojibake(String rawText) {
        if (rawText == null || rawText.isEmpty()) return rawText;
        try {
            return new String(rawText.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return rawText;
        }
    }
    private List<HSQTools.TextBlock> clickMultiInput(String labelToFind, String rawValues)
    {
        List<HSQTools.TextBlock> tempCompare = new ArrayList<>();
        int vuotLenLai = 0;
        String[] valuesToInput = rawValues.split("\\^");

        while (true)
        {
            List<HSQTools.TextBlock> currentScreen = getCheckAnswerSmart();
            phoneScreen.addAll(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
            phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));
            List<android.graphics.Rect> inputBoxes = new ArrayList<>();

            String xml = HSQTools.getFlexibleXML();
            try
            {
                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                String normTarget = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(labelToFind));
                int labelBottom = -1;
                int minLabelHeight = 99999;

                // 1. Tìm Mỏ Neo (Y chang clickInput xịn)
                for (int i = 0; i < nodes.getLength(); i++) {
                    org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                    String clazz = node.getAttribute("class");
                    if (!clazz.contains("EditText") && !clazz.contains("Button") && !clazz.contains("WebView")) {
                        String combined = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(node.getAttribute("text") + " " + node.getAttribute("content-desc")));
                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                        if (r != null && !combined.isEmpty() && r.height() < (heightOfScreen * 0.8) && normTarget.length() >= 2) {
                            if (combined.equals(normTarget) || combined.contains(normTarget)) {
                                if (r.height() < minLabelHeight) {
                                    minLabelHeight = r.height();
                                    labelBottom = r.bottom;
                                }
                            }
                        }
                    }
                }

                // 2. Gom TẤT CẢ EditText nằm dưới Mỏ Neo vào mảng (Hoặc TẤT CẢ nếu không có Mỏ Neo)
                for (int i = 0; i < nodes.getLength(); i++) {
                    org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                    if (node.getAttribute("class").contains("EditText") || node.getAttribute("class").contains("AutoCompleteTextView")) {
                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                        // Mở rộng bán kính quét lên 2000px để gom trọn ổ các hộp phía dưới
                        // NẾU MỎ NEO MÙ (labelBottom == -1), TA GOM SẠCH SÀNH SANH CÁC Ô NHẬP LIỆU TRÊN MÀN HÌNH
                        if (r != null && (labelBottom == -1 || (r.top >= labelBottom - 50 && r.top <= labelBottom + 2000))) {
                            if (r.centerY() > 180 && r.centerY() < heightOfScreen - 50) {

                                // Chống đếm trùng (Bọn WebView thỉnh thoảng nhả 2 Node đè lên nhau cho 1 ô)
                                boolean isDuplicate = false;
                                for (android.graphics.Rect box : inputBoxes) {
                                    if (Math.abs(box.centerY() - r.centerY()) < 30) {
                                        isDuplicate = true; break;
                                    }
                                }
                                if (!isDuplicate) inputBoxes.add(r);
                            }
                        }
                    }
                }

                // =========================================================
                // 🔥 BỌC THÉP TẦNG CUỐI: FALLBACK OCR CHO WEBVIEW MÙ
                // =========================================================
                if (inputBoxes.isEmpty()) {
                    normTarget = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(labelToFind));
                    HSQTools.TextBlock labelBlock = null;
                    int minLabelY = 99999;

                    for (HSQTools.TextBlock tb : currentScreen) {
                        if (tb.y > 180) {
                            String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(tb.text));
                            if (!clean.isEmpty() && normTarget.length() >= 2 && (clean.equals(normTarget) || clean.contains(normTarget))) {
                                if (tb.y < minLabelY) {
                                    minLabelY = tb.y;
                                    labelBlock = tb;
                                }
                            }
                        }
                    }

                    if (labelBlock != null) {
                        updateNotificationContent("WebView mù! Kích hoạt chế độ Dò Mìn OCR...");
                        int currentY = labelBlock.y + 120; // Khởi đầu dưới Label 120px

                        // Lấy tất cả các cục text nằm bên dưới Label
                        List<HSQTools.TextBlock> belowTexts = new ArrayList<>();
                        for (HSQTools.TextBlock tb : currentScreen) {
                            if (tb.y > labelBlock.y + 20 && tb.y < heightOfScreen - 200) {
                                String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(tb.text));
                                // Loại bỏ mấy nút Next hoặc text rác
                                if (!clean.matches("^(next|continue|tieptuc|submit|done)$") && !clean.contains("khongynaoneutren") && !clean.contains("noneof")) {
                                    belowTexts.add(tb);
                                }
                            }
                        }

                        belowTexts.sort(Comparator.comparingInt(t -> t.y));

                        if (!belowTexts.isEmpty()) {
                            for (HSQTools.TextBlock tb : belowTexts) {
                                // 🌟 TẠO TỌA ĐỘ BẮN MÙ XUYÊN TÂM
                                // Text "Other" nằm rìa trái, ô nhập thường trải dài qua giữa màn hình -> Bắn thẳng xCenter
                                android.graphics.Rect virtualBox = new android.graphics.Rect(xCenter - 50, tb.y - 20, xCenter + 50, tb.y + 20);
                                inputBoxes.add(virtualBox);
                            }

                            // Đề phòng các ô ở dưới không có chữ mồi, bơm thêm vài tọa độ mù phía dưới cùng
                            int lastY = belowTexts.get(belowTexts.size() - 1).y;
                            for(int i = 1; i <= 3; i++) {
                                inputBoxes.add(new android.graphics.Rect(xCenter - 50, lastY + (i * 120) - 20, xCenter + 50, lastY + (i * 120) + 20));
                            }
                        } else {
                            // Nếu đéo có chữ gì dưới Label, nã đạn thẳng xuống dưới mỗi 130px
                            for (int i = 0; i < valuesToInput.length + 3; i++) {
                                inputBoxes.add(new android.graphics.Rect(xCenter - 50, currentY - 20, xCenter + 50, currentY + 20));
                                currentY += 130;
                            }
                        }
                    }
                }

                // 3. THỰC THI NHẬP LIỆU HÀNG LOẠT (CHẾ ĐỘ DÒ MÌN)
                if (!inputBoxes.isEmpty()) {
                    // Sắp xếp các ô từ Trên xuống Dưới theo trục Y cho chuẩn xác
                    inputBoxes.sort(Comparator.comparingInt(r -> r.top));

                    int successfulInputs = 0;
                    int targetCount = valuesToInput.length;

                    updateNotificationContent("Multi-Input: Bắt được " + inputBoxes.size() + " ô ảo. Bắt đầu rải thảm...");

                    for (int i = 0; i < inputBoxes.size(); i++) {
                        if (successfulInputs >= targetCount) break;

                        android.graphics.Rect box = inputBoxes.get(i);
                        String textToInput = valuesToInput[successfulInputs].trim();
                        if(textToInput.isEmpty()) {
                            successfulInputs++;
                            continue;
                        }

                        click(box.centerX(), box.centerY(), false);
                        delay(2500);

                        // TRỌNG TÀI BÀN PHÍM
                        if (HSQTools.isKeyboardVisibleSmart()) {
                            clearAllText();
                            delay(500);
                            inputText(textToInput, null, true);
                            delay(1500);
                            successfulInputs++;

                            globalBack(); // Đóng bàn phím để lộ màn hình click ô tiếp theo
                            delay(1500);
                        }
                        // Nếu đéo nảy bàn phím -> Kệ mẹ, đó là ta click nhầm dòng chữ rác (Vd: "Vui lòng điền một thương hiệu..."). Vòng lặp sẽ tiếp tục thử ô thấp hơn!
                    }

                    if (successfulInputs > 0) {
                        return null; // Ít nhất nhập được 1 cái là THÀNH CÔNG RỰC RỠ!
                    } else {
                        return currentScreen; // Lỗi đéo mở được bàn phím cái nào
                    }
                }
            } catch (Exception ignored) {}

            // =========================================================
            // 4. FALLBACK VUỐT TÌM KIẾM
            // =========================================================
            List<HSQTools.TextBlock> currentVisible = currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList());
            if (HSQTools.areAlmostSame(tempCompare, currentVisible, 20)) {
                if (vuotLenLai == 0) {
                    vuotLenLai++;
                } else {
                    return currentScreen;
                }
            }
            tempCompare = new ArrayList<>(currentVisible);
            if (vuotLenLai == 0) smartScroll(ysBot, ysTop, "input");
            else smartScroll(ysTop, ysBot, "input");
            delay(2500);
        }
    }
    private void getTopText(List<HSQTools.TextBlock> screenTop) {
        for(int i = 0; i < screenTop.size(); i++) {
            String TestText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(screenTop.get(i).text.toLowerCase()));
            if(TestText.length() > 6) {
                topText = TestText;
                return;
            }
        }
    }
    private void swipeToTop(int slVuot, boolean isdropdown) {
        if(slVuot > 0) {
            for(int j = 0; j < slVuot; j++) {
                if(j == slVuot - 1 && !isdropdown) {
                    List< HSQTools.TextBlock> smartList = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                    for(int k = 0; k < smartList.size(); k++) {
                        if(HSQTools.getOnlyTextLinq(HSQTools.normalizeText(smartList.get(k).text.toLowerCase())).equals(topText)) {
                            return;
                        }
                    }
                }
                if (isdropdown) swipe(xs, ysTop, xs, ysBot, swipeDuration);
                else smartScroll(ysTop, ysBot, "swipemore_back");
                delay(2000);
            }
            if(!isdropdown)
            {
                List<HSQTools.TextBlock> smartList = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                for (int k = 0; k < smartList.size(); k++)
                {
                    if (HSQTools.getOnlyTextLinq(HSQTools.normalizeText(smartList.get(k).text.toLowerCase())).equals(topText))
                    {
                        return;
                    }
                }
            }
            if (isdropdown) swipe(xs, ysTop, xs, ysBot, swipeDuration);
            else smartScroll(ysTop, ysBot, "swipemore_back");
            delay(2000);
        }
    }
    private List<HSQTools.TextBlock> clickSlider(String labelToFind, int percent)
    {
        String xml;
        List<HSQTools.TextBlock> tempCompare = new ArrayList<>();
        int vuotLenLai = 0;

        while (true)
        {
            List<HSQTools.TextBlock> currentScreen = getCheckAnswerSmart();
            phoneScreen.addAll(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()));
            phoneScreen.add(new HSQTools.TextBlock("Đã vuốt xuống", 0, 0));
            int clickX = -1, clickY = -1;
            String normTarget = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(labelToFind));

            // ========================================================
            // 🎯 1. TÌM MỐC TIÊU ĐỀ (LABEL) BẰNG MẮT THẦN (CHỐNG GHOST NODE 100%)
            // Thay vì tin XML, ta dùng OCR/ASBL để tóm chính xác Y của chữ đang hiển thị
            // ========================================================
            HSQTools.TextBlock anchorNode = currentScreen.stream()
                    .filter(x -> x.y > 180 && x.y < heightOfScreen - 50)
                    .filter(x ->
                    {
                        String cleanText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text));
                        if (cleanText.isEmpty()) return false;

                        if (HSQTools.equalsOcrFriendly(cleanText, normTarget)) return true;

                        if (normTarget.length() >= 3 && HSQTools.containsOcrFriendly(cleanText, normTarget)) return true;

                        if (cleanText.length() >= 5 && HSQTools.containsOcrFriendly(normTarget, cleanText)) return true;

                        if (cleanText.length() >= 10 && normTarget.length() >= 10)
                        {
                            int lenDiff = Math.abs(cleanText.length() - normTarget.length());
                            int dist = HSQTools.levenshtein(cleanText, normTarget);
                            int allow = Math.max(1, (int) (Math.min(cleanText.length(), normTarget.length()) * 0.12f));

                            return lenDiff <= 4 && dist <= allow;
                        }

                        return false;
                    })
                    .min(Comparator
                            .comparingInt((HSQTools.TextBlock x) ->
                            {
                                String cleanText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text));

                                if (HSQTools.equalsOcrFriendly(cleanText, normTarget)) return 0;
                                if (normTarget.length() >= 3 && HSQTools.containsOcrFriendly(cleanText, normTarget)) return 1;
                                if (cleanText.length() >= 5 && HSQTools.containsOcrFriendly(normTarget, cleanText)) return 2;
                                return 3;
                            })
                            .thenComparingInt(x ->
                            {
                                String cleanText = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(x.text));
                                return Math.abs(cleanText.length() - normTarget.length());
                            }))
                    .orElse(null);

            // ========================================================
            // 🎯 2. QUÉT XML TÌM THANH TRƯỢT NẰM NGAY DƯỚI EXACT Y
            // ========================================================
            if (anchorNode != null) {
                int exactY = anchorNode.y;
                xml = HSQTools.getFlexibleXML();
                try {
                    javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                    javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                    org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                    int bestY = -1;
                    int bestStartX = -1;
                    int bestWidth = -1;
                    int minDistance = 9999;
                    int safePercent = Math.max(5, Math.min(95, percent));
                    
                    java.util.List<android.graphics.Rect> xmlNumberNodes = new java.util.ArrayList<>();

                    for (int i = 0; i < nodes.getLength(); i++) {
                        org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                        if (r != null && r.centerY() > 180 && r.centerY() < heightOfScreen - 50) {
                            int distance = r.top - exactY;

                            // Chỉ quét những thằng nằm dưới mỏ neo (sai số -30px), tối đa 65% chiều cao màn hình
                            if (distance >= -30 && distance < heightOfScreen * 0.65f) {
                                String clazz = node.getAttribute("class");
                                boolean isClickable = node.getAttribute("clickable").equals("true");
                                boolean isFocusable = node.getAttribute("focusable").equals("true");

                                // 🌟 LOẠI 1: NẾU TÌM THẤY CLASS SEEKBAR CHUẨN
                                if (clazz.contains("SeekBar") || clazz.contains("ProgressBar")) {
                                    if (distance < minDistance) {
                                        minDistance = distance;
                                        bestY = r.centerY();
                                        bestStartX = r.left;
                                        bestWidth = r.width();
                                    }
                                }
                                // 🌟 LOẠI 2 (Ý TƯỞNG CỦA SẾP): ĐƯỜNG RAY TƯƠNG TÁC CHỨA SỐ / RÃNH
                                // Bọc thép: Phải rộng (> 40% màn hình) VÀ bắt buộc phải bấm/chạm được (Interactive).
                                // Điều này vứt bỏ 100% các thẻ text rác (như "Chắc chắn không") vì chúng đéo focusable!
                                else if (r.width() > (widthOfScreen * 0.4) && (isClickable || isFocusable)) {
                                    if (distance < minDistance) {
                                        minDistance = distance;
                                        bestY = r.centerY();
                                        bestStartX = r.left;
                                        bestWidth = r.width();
                                    }
                                }
                                // 🌟 LOẠI 3: THANH TRƯỢT TRONG WEBVIEW (BỊ MẤT CLICKABLE)
                                // Đặc điểm: Rất rộng (>= 60% màn hình) và Rất mỏng (height <= 60px)
                                else if (r.width() > (widthOfScreen * 0.6) && r.height() > 0 && r.height() <= 60) {
                                    if (distance < minDistance) {
                                        minDistance = distance;
                                        bestY = r.centerY();
                                        bestStartX = r.left;
                                        bestWidth = r.width();
                                    }
                                }
                                // 🌟 TÍCH LŨY NODE ĐỂ TÌM LOẠI 3.5 (XML HORIZONTAL GROUP)
                                // Lưu lại các thẻ RadioButton, CheckBox hoặc BẤT CỨ thẻ nào chứa duy nhất số nguyên để gom nhóm
                                if (clazz.contains("RadioButton") || clazz.contains("CheckBox") || node.getAttribute("text").matches("^\\d+$")) {
                                    xmlNumberNodes.add(r);
                                }
                            }
                        }
                    }

                    // 🌟 LOẠI 3.5 (ĐỘT PHÁ MỚI): GOM NHÓM CÁC NÚT BẤM / RADIO TRONG XML THEO HÀNG NGANG
                    // Nếu WebView có chứa hàng Radio từ 0->10, ta bắt chính xác tuyệt đối mà không cần OCR!
                    boolean isExactWidth = false;

                    if (!xmlNumberNodes.isEmpty()) { // Xóa check bestY == -1, ÉP GHI ĐÈ Loại 2 nếu tìm thấy Scale!
                        int maxGroupSize = 0;
                        java.util.List<android.graphics.Rect> bestGroup = null;
                        
                        for (android.graphics.Rect pivot : xmlNumberNodes) {
                            java.util.List<android.graphics.Rect> group = xmlNumberNodes.stream()
                                    .filter(x -> Math.abs(x.centerY() - pivot.centerY()) <= 40)
                                    .collect(Collectors.toList());
                            if (group.size() > maxGroupSize) {
                                maxGroupSize = group.size();
                                bestGroup = group;
                            }
                        }

                        // Yêu cầu nhóm phải có ít nhất 3 phần tử (để chắc chắn là 1 hàng scale chứ ko phải nút linh tinh)
                        if (bestGroup != null && bestGroup.size() >= 3) {
                            int minX = bestGroup.stream().mapToInt(Rect::centerX).min().orElse(-1);
                            int maxX = bestGroup.stream().mapToInt(Rect::centerX).max().orElse(-1);

                            if (maxX - minX > widthOfScreen * 0.3) {
                                bestY = bestGroup.get(0).centerY();
                                bestStartX = minX;
                                bestWidth = maxX - minX;
                                isExactWidth = true; // Kích thước kim cương, cấm thụt lề!
                            }
                        }
                    }

                    // 🌟 LOẠI 4 (VŨ KHÍ OCR): TÌM HÀNG SỐ DƯỚI LABEL NHƯ Ý SẾP
                    // Quét các text chứa số nằm dưới Label, nếu tạo thành 1 hàng ngang đủ rộng -> Lấy tọa độ!
                    if (bestY == -1) {
                        java.util.List<HSQTools.TextBlock> numbers = currentScreen.stream()
                                .filter(x -> x.y > exactY && x.y < exactY + (heightOfScreen * 0.65f)) // Nới lỏng khoảng cách Y xuống 65% màn hình
                                .filter(x -> x.text.matches(".*\\d.*") && x.text.replaceAll("[\\d\\s.,\\-]", "").length() < 15) // Bỏ qua nếu là câu văn dài (như tiêu đề chứa số)
                                .collect(Collectors.toList());

                        int maxGroupSize = 0;
                        java.util.List<HSQTools.TextBlock> bestGroup = null;

                        for (HSQTools.TextBlock pivot : numbers) {
                            java.util.List<HSQTools.TextBlock> group = numbers.stream()
                                    .filter(x -> Math.abs(x.y - pivot.y) <= 40)
                                    .collect(Collectors.toList());
                            if (group.size() > maxGroupSize) {
                                maxGroupSize = group.size();
                                bestGroup = group;
                            }
                        }

                        if (bestGroup != null) {
                            if (bestGroup.size() >= 2) {
                                int minX = bestGroup.stream().mapToInt(x -> x.x).min().orElse(-1);
                                int maxX = bestGroup.stream().mapToInt(x -> x.x).max().orElse(-1);

                                if (maxX - minX > widthOfScreen * 0.3) {
                                    bestY = bestGroup.get(0).y; // Click thẳng vào giữa hàng số! Không nâng Y nữa.
                                    bestStartX = minX;
                                    bestWidth = maxX - minX;
                                    isExactWidth = true; // Cắm cờ để giữ nguyên width, đéo bị thụt lề!
                                }
                            } else if (bestGroup.size() == 1) {
                                // 🌟 TRƯỜNG HỢP SẾP BẮT BÀI: Mắt thần gộp chung 1 nùi số "01 2 3 4 5 67 8 9 10 56"
                                String t = bestGroup.get(0).text;
                                if (t.length() > 5 && t.replaceAll("[^0-9]", "").length() >= 3) {
                                    bestY = bestGroup.get(0).y; // Lấy đúng Y của cụm số này
                                    // Sếp nói đúng, mặc dù OCR gộp chuỗi nhưng trên giao diện các ô số kéo dãn đều ra 2 bên!
                                    // Thế nên ta áp dụng ĐÚNG CÔNG THỨC CỦA SẾP:
                                    // Thụt lề trái 10% (StartX) -> Bề ngang bằng Tổng trừ đi 2 cái lề đó!
                                    bestStartX = (int) (widthOfScreen * 0.1);
                                    bestWidth = widthOfScreen - (bestStartX * 2);
                                }
                            }
                        }

                        // Nếu vã quá đéo có số nào, thì mới dùng Bất đắc dĩ
                        if (bestY == -1) {
                            bestY = exactY + 250;
                            bestStartX = (int) (widthOfScreen * 0.1);
                            bestWidth = widthOfScreen - (bestStartX * 2);
                        }
                    }

                    // 🎯 TÍNH TOÁN TỌA ĐỘ CHỌT X TỪ ĐƯỜNG RAY TÌM ĐƯỢC
                    if (bestY != -1) {
                        clickY = bestY;
                        // Xử lý Padding: Lề phải phải thụt vào đúng bằng lề trái (StartX)
                        // Bề ngang thực tế = Bề ngang màn hình - (Lề trái * 2)
                        if (!isExactWidth && bestWidth > widthOfScreen * 0.85) {
                            // Nếu quét XML ra thanh trượt tràn lề, tự khống chế thụt 10%
                            bestStartX = bestStartX + (int)(bestWidth * 0.1);
                            bestWidth = (int)(bestWidth * 0.8);
                        }

                        clickX = bestStartX + (int) (bestWidth * (safePercent / 100.0f));
                    }
                } catch (Exception ignored) {}
            }

            // =========================================================
            // 🎯 3. THỰC THI CHỌT THANH TRƯỢT
            // =========================================================
            if (clickX != -1 && clickY != -1) {
                updateNotificationContent("Chọt thanh trượt [" + normTarget + "] mức " + percent + "% tại X=" + clickX + ", Y=" + clickY);
                click(clickX, clickY, false);
                delay(2000); // Chờ UI nó nhảy cục xanh tới chỗ chọt
                return null; // THÀNH CÔNG RỰC RỠ!
            }

            // =========================================================
            // 🎯 3.5 VUỐT MỒI: THẤY ĐẦU MÀ CHƯA THẤY ĐUÔI
            // Nếu thấy tiêu đề nhưng đéo thấy thanh trượt -> Nhích màn hình lên 1 đoạn ngắn!
            // =========================================================
            if (anchorNode != null && clickX == -1) {
                updateNotificationContent("Thấy tiêu đề nhưng thanh trượt còn kẹt dưới đáy. Nhích lên xíu...");
                // Vuốt một đoạn ngắn (Từ ysBot lên ysBot - 800) để lôi thanh trượt lên
                swipe(xs, ysBot, xs, ysBot - 800, 1000);
                delay(2000);

                // Xóa trí nhớ màn hình để vòng lặp sau không bị kẹt Logic Đụng Đáy của Tầng 4
                tempCompare.clear();
                continue; // Quay lại đầu vòng lặp để chốt hạ!
            }

            // =========================================================
            // 🎯 4. FALLBACK VUỐT TÌM KIẾM
            // =========================================================
            List<HSQTools.TextBlock> currentVisible = currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList());
            if (HSQTools.areAlmostSame(tempCompare, currentVisible, 20)) {
                if (vuotLenLai == 0) vuotLenLai++;
                else return currentScreen; // Cuộn nát máy đéo thấy -> Báo lỗi
            }
            tempCompare = new ArrayList<>(currentVisible);

            if (vuotLenLai == 0) swipe(xs, ysBot, xs, ysTop, swipeDuration);
            else swipe(xs, ysTop, xs, ysBot, swipeDuration);

            delay(2500);
        }
    }
    // Tạo một cái Map toàn cục để nhớ xem Số nào đi với Tọa độ nào
    public static java.util.Map<Integer, android.graphics.Point> somMap = new java.util.HashMap<>();

    public static android.graphics.Bitmap generateSoMImage(List<HSQTools.TextBlock> targets) {
        android.graphics.Bitmap rawScreen = HSQTools.getScreenBitmap();
        if (rawScreen == null) return null;

        android.graphics.Bitmap mutableBitmap = rawScreen.copy(android.graphics.Bitmap.Config.ARGB_8888, true);
        android.graphics.Canvas canvas = new android.graphics.Canvas(mutableBitmap);

        // --- SETUP BỘ CỌ VẼ ---
        android.graphics.Paint boxPaint = new android.graphics.Paint();
        boxPaint.setColor(android.graphics.Color.RED);
        boxPaint.setStyle(android.graphics.Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5f);

        android.graphics.Paint bgPaint = new android.graphics.Paint();
        bgPaint.setColor(android.graphics.Color.RED);
        bgPaint.setStyle(android.graphics.Paint.Style.FILL);

        android.graphics.Paint textPaint = new android.graphics.Paint();
        textPaint.setColor(android.graphics.Color.WHITE);
        textPaint.setTextSize(45f);
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));

        somMap.clear();

        // 🌟 MỚI: Lấy XML để tính bounds thật cho mỗi node
        java.util.Map<Integer, android.graphics.Rect> xmlBoundsCache = new java.util.HashMap<>();
        try {
            String xml = HSQTools.getFlexibleXML();
            javax.xml.parsers.DocumentBuilder builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                if (r != null && r.width() > 0 && r.height() > 0) {
                    // Cache theo centerY để mapping với TextBlock
                    xmlBoundsCache.put(r.centerY() * 10000 + r.centerX(), r);
                }
            }
        } catch (Exception ignored) {}

        int id = 1;
        for (HSQTools.TextBlock node : targets) {
            // 🌟 THỬ TÌM BOUNDS THẬT TỪ XML (Sai số 50px)
            android.graphics.Rect bestMatch = null;
            int bestDist = Integer.MAX_VALUE;
            for (java.util.Map.Entry<Integer, android.graphics.Rect> entry : xmlBoundsCache.entrySet()) {
                android.graphics.Rect r = entry.getValue();
                int dist = Math.abs(r.centerY() - node.y) + Math.abs(r.centerX() - node.x);
                if (dist < bestDist && dist < 100) {
                    bestDist = dist;
                    bestMatch = r;
                }
            }

            int left, top, right, bottom;
            if (bestMatch != null) {
                // Dùng bounds thật từ XML, thêm padding 10px
                left = bestMatch.left - 10;
                top = bestMatch.top - 10;
                right = bestMatch.right + 10;
                bottom = bestMatch.bottom + 10;
            } else {
                // Fallback: ước lượng dựa trên độ dài text
                int estimatedWidth = Math.max(120, node.text.length() * 28);
                left = node.x - estimatedWidth / 2;
                top = node.y - 50;
                right = node.x + estimatedWidth / 2;
                bottom = node.y + 50;
            }

            // Ép không vẽ ra ngoài màn hình
            if (left < 0) left = 0;
            if (top < 50) top = 50;
            if (right > mutableBitmap.getWidth()) right = mutableBitmap.getWidth();
            if (bottom > mutableBitmap.getHeight()) bottom = mutableBitmap.getHeight();

            // Vẽ Khung Đỏ
            canvas.drawRect(left, top, right, bottom, boxPaint);

            // Vẽ Nền Đỏ cho Nhãn số (góc trên trái, tự co giãn theo số chữ số)
            String idStr = String.valueOf(id);
            float textWidth = textPaint.measureText(idStr);
            canvas.drawRect(left, top - 55, left + textWidth + 20, top, bgPaint);

            // Vẽ Số ID
            canvas.drawText(idStr, left + 10, top - 12, textPaint);

            somMap.put(id, new android.graphics.Point(node.x, node.y));
            id++;
        }

        rawScreen.recycle();
        return mutableBitmap;
    }

    public static void saveBitmapToFile(android.graphics.Bitmap bitmap, String filePath) {
        while(true)
        {
            if (bitmap == null) return;
            java.io.File file = new java.io.File(filePath);
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(file))
            {
                // Nén thành file PNG, chất lượng 100%
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                return;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            delay(5000);
        }
    }

    private boolean isClickAnswerMatchOcrFriendly(String rawNodeText, String rawAnswer, boolean allowContains)
    {
        String node = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawNodeText));
        String answer = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawAnswer));

        if (node.isEmpty() || answer.isEmpty()) return false;
        if (matchesAnswerCandidateOcrFriendly(node, answer, allowContains)) return true;

        // OCR hay đọc vòng radio trước đáp án thành "O" hoặc "0": O2, OVeryGood...
        String strippedNode = stripLeadingRadioGlyph(node);
        return !strippedNode.equals(node) && matchesAnswerCandidateOcrFriendly(strippedNode, answer, allowContains);
    }

    private HSQTools.TextBlock findClickBlockAnswerNode(
            List<HSQTools.TextBlock> currentVisible,
            String cleanAns,
            int finalTargetY,
            boolean allowContains)
    {
        List<HSQTools.TextBlock> candidates = new ArrayList<>();

        for (HSQTools.TextBlock x : currentVisible)
        {
            if (!isInsideClickBlockAnswerBand(x, finalTargetY)) continue;
            if (isClickAnswerMatchOcrFriendly(x.text, cleanAns, allowContains))
            {
                candidates.add(x);
            }
        }

        // getCheckAnswerSmart co the loc mat dap an 1 ky tu nhu "6", "B".
        if (candidates.isEmpty() && cleanAns.length() <= 2)
        {
            try
            {
                List<HSQTools.TextBlock> rawOcr = HSQTools.getOcrTextBlocks();
                for (HSQTools.TextBlock x : rawOcr)
                {
                    if (!isInsideClickBlockAnswerBand(x, finalTargetY)) continue;
                    if (isClickAnswerMatchOcrFriendly(x.text, cleanAns, allowContains))
                    {
                        candidates.add(x);
                    }
                }
            }
            catch (Exception ignored)
            {
            }
        }

        return candidates.stream()
                .min(Comparator
                        .comparingInt((HSQTools.TextBlock x) -> finalTargetY != -1 ? Math.abs(x.y - finalTargetY) : x.y)
                        .thenComparingInt(x -> Math.abs(x.x - xCenter)))
                .orElse(null);
    }

    private boolean isInsideClickBlockAnswerBand(HSQTools.TextBlock x, int finalTargetY)
    {
        if (x == null) return false;

        if (finalTargetY != -1)
        {
            return x.y > finalTargetY + 20 && x.y < finalTargetY + 1500;
        }

        return x.y > 180 && x.y < heightOfScreen - 80;
    }
    private android.graphics.Point findClickBlockMatrixChoicePointFromXml(String headerStr, String answerStr, int targetY, List<HSQTools.TextBlock> currentVisible)
    {
        try
        {
            String xml = HSQTools.getFlexibleXML();
            if (xml == null || xml.trim().isEmpty()) return null;

            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

            List<android.graphics.Rect> radioRects = collectClickBlockChoiceRects(nodes);
            List<List<android.graphics.Rect>> rowGroups = groupClickBlockChoiceRows(radioRects);
            if (rowGroups.isEmpty()) return null;

            List<android.graphics.Rect> row = findClickBlockMatrixChoiceRowFromXmlLabel(nodes, headerStr, targetY);
            if (row == null)
            {
                row = chooseClickBlockChoiceRow(rowGroups, targetY);
            }
            if (row == null || row.size() < 2) return null;
            row.sort(Comparator.comparingInt(android.graphics.Rect::centerX));

            int rowY = averageRectCenterY(row);
            int targetX = findClickBlockColumnHeaderX(nodes, answerStr, rowY);
            if (targetX <= 0)
            {
                targetX = guessClickBlockColumnX(row, answerStr);
            }
            if (targetX <= 0) return null;

            final int targetXFinal = targetX;
            android.graphics.Rect best = row.stream()
                    .min(Comparator.comparingInt(r -> Math.abs(r.centerX() - targetXFinal)))
                    .orElse(null);
            if (best == null) return null;

            updateNotificationContent("Block XML: chot [" + headerStr + "~" + answerStr + "] tai " + best.centerX() + "," + best.centerY());
            return new android.graphics.Point(best.centerX(), best.centerY());
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private List<android.graphics.Rect> findClickBlockMatrixChoiceRowFromXmlLabel(org.w3c.dom.NodeList nodes, String headerStr, int targetY)
    {
        if (nodes == null || headerStr == null || headerStr.trim().isEmpty()) return null;

        int bestScore = Integer.MIN_VALUE;
        List<android.graphics.Rect> bestRow = null;

        for (int i = 0; i < nodes.getLength(); i++)
        {
            org.w3c.dom.Node domNode = nodes.item(i);
            if (!(domNode instanceof org.w3c.dom.Element)) continue;
            org.w3c.dom.Element node = (org.w3c.dom.Element) domNode;

            android.graphics.Rect labelRect = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
            if (labelRect == null || labelRect.width() <= 0 || labelRect.height() <= 0) continue;
            if (labelRect.centerY() <= 180 || labelRect.centerY() >= heightOfScreen - 80) continue;
            if (labelRect.centerX() > widthOfScreen * 0.46f) continue;

            String rawText = (node.getAttribute("text") + " " + node.getAttribute("content-desc")).trim();
            if (rawText.trim().isEmpty()) continue;

            org.w3c.dom.Node parent = node.getParentNode();
            if (parent == null) continue;

            List<android.graphics.Rect> choices = collectClickBlockChoiceRects(parent);
            if (choices.size() < 2) continue;
            choices.sort(Comparator.comparingInt(android.graphics.Rect::centerX));

            int rowY = averageRectCenterY(choices);
            int labelScore = clickBlockMatrixLabelScore(rawText, headerStr);
            int dy = targetY > 0 ? Math.abs(rowY - targetY) : Math.abs(rowY - labelRect.centerY());

            if (labelScore <= 0 && (targetY <= 0 || dy > 280)) continue;

            int score = labelScore * 1000;
            if (targetY > 0) score += Math.max(0, 800 - dy);
            score -= Math.abs(rowY - labelRect.centerY()) / 2;

            if (score > bestScore)
            {
                bestScore = score;
                bestRow = choices;
            }
        }

        return bestRow;
    }

    private int clickBlockMatrixLabelScore(String rawNodeText, String rawTarget)
    {
        String node = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawNodeText));
        String target = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawTarget));
        if (node.isEmpty() || target.isEmpty()) return 0;

        if (node.equals(target)) return 10;
        if (node.contains(target) || target.contains(node)) return 8;

        int dist = HSQTools.levenshtein(node, target);
        if (dist <= Math.max(2, (int) (Math.min(node.length(), target.length()) * 0.22f))) return 7;

        List<String> nodeTokens = clickBlockLooseTokens(rawNodeText);
        List<String> targetTokens = clickBlockLooseTokens(rawTarget);
        if (nodeTokens.isEmpty() || targetTokens.isEmpty()) return 0;

        int hits = 0;
        for (String t : targetTokens)
        {
            if (t.length() <= 1) continue;
            for (String n : nodeTokens)
            {
                if (n.equals(t) || n.length() >= 3 && t.length() >= 3 && (n.contains(t) || t.contains(n)))
                {
                    hits++;
                    break;
                }
            }
        }

        if (hits >= Math.max(2, Math.min(4, targetTokens.size() / 2))) return 4 + Math.min(3, hits);
        return 0;
    }

    private List<String> clickBlockLooseTokens(String raw)
    {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        String noAccent = HSQTools.removeAccents(raw).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        if (noAccent.isEmpty()) return out;
        for (String token : noAccent.split("\\s+"))
        {
            if (token.length() > 0) out.add(token);
        }
        return out;
    }

    private List<android.graphics.Rect> collectClickBlockChoiceRects(org.w3c.dom.NodeList nodes)
    {
        List<android.graphics.Rect> result = new ArrayList<>();
        if (nodes == null) return result;

        for (int i = 0; i < nodes.getLength(); i++)
        {
            org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
            android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
            if (r == null || r.width() <= 0 || r.height() <= 0) continue;
            if (r.centerY() <= 180 || r.centerY() >= heightOfScreen - 80) continue;
            if (r.centerX() < widthOfScreen * 0.32f) continue;
            if (r.width() < 24 || r.height() < 24 || r.width() > 240 || r.height() > 240) continue;

            String clazz = node.getAttribute("class");
            String text = node.getAttribute("text");
            String desc = node.getAttribute("content-desc");
            boolean textEmpty = (text == null || text.trim().isEmpty()) && (desc == null || desc.trim().isEmpty());
            boolean checkable = "true".equals(node.getAttribute("checkable"))
                    || (clazz != null && (clazz.contains("RadioButton") || clazz.contains("CheckBox") || clazz.contains("CompoundButton")));
            boolean customChoice = "true".equals(node.getAttribute("clickable"))
                    && textEmpty
                    && Math.abs(r.width() - r.height()) <= Math.max(24, Math.min(r.width(), r.height()) * 0.45f);

            if (!checkable && !customChoice) continue;
            if (isNearExistingChoiceRect(result, r)) continue;
            result.add(r);
        }

        result.sort(Comparator.comparingInt(android.graphics.Rect::centerY).thenComparingInt(android.graphics.Rect::centerX));
        return result;
    }

    private List<android.graphics.Rect> collectClickBlockChoiceRects(org.w3c.dom.Node parent)
    {
        List<android.graphics.Rect> result = new ArrayList<>();
        if (parent == null) return result;

        org.w3c.dom.NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            collectClickBlockChoiceRectsRec(children.item(i), result);
        }

        result.sort(Comparator.comparingInt(android.graphics.Rect::centerY).thenComparingInt(android.graphics.Rect::centerX));
        return result;
    }

    private void collectClickBlockChoiceRectsRec(org.w3c.dom.Node domNode, List<android.graphics.Rect> result)
    {
        if (!(domNode instanceof org.w3c.dom.Element)) return;
        org.w3c.dom.Element node = (org.w3c.dom.Element) domNode;

        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
        if (isClickBlockChoiceNode(node, r) && !isNearExistingChoiceRect(result, r))
        {
            result.add(r);
        }

        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            collectClickBlockChoiceRectsRec(children.item(i), result);
        }
    }

    private boolean isClickBlockChoiceNode(org.w3c.dom.Element node, android.graphics.Rect r)
    {
        if (node == null || r == null || r.width() <= 0 || r.height() <= 0) return false;
        if (r.centerY() <= 180 || r.centerY() >= heightOfScreen - 80) return false;
        if (r.centerX() < widthOfScreen * 0.32f) return false;
        if (r.width() < 24 || r.height() < 24 || r.width() > 240 || r.height() > 240) return false;

        String clazz = node.getAttribute("class");
        String text = node.getAttribute("text");
        String desc = node.getAttribute("content-desc");
        boolean textEmpty = (text == null || text.trim().isEmpty()) && (desc == null || desc.trim().isEmpty());
        boolean checkable = "true".equals(node.getAttribute("checkable"))
                || (clazz != null && (clazz.contains("RadioButton") || clazz.contains("CheckBox") || clazz.contains("CompoundButton")));
        boolean customChoice = "true".equals(node.getAttribute("clickable"))
                && textEmpty
                && Math.abs(r.width() - r.height()) <= Math.max(24, Math.min(r.width(), r.height()) * 0.45f);

        return checkable || customChoice;
    }

    private boolean isNearExistingChoiceRect(List<android.graphics.Rect> rects, android.graphics.Rect candidate)
    {
        for (android.graphics.Rect r : rects)
        {
            if (Math.abs(r.centerX() - candidate.centerX()) <= 28 && Math.abs(r.centerY() - candidate.centerY()) <= 28)
            {
                return true;
            }
        }
        return false;
    }

    private List<List<android.graphics.Rect>> groupClickBlockChoiceRows(List<android.graphics.Rect> rects)
    {
        List<List<android.graphics.Rect>> rows = new ArrayList<>();
        if (rects == null || rects.isEmpty()) return rows;

        for (android.graphics.Rect r : rects)
        {
            List<android.graphics.Rect> bestRow = null;
            int bestDist = Integer.MAX_VALUE;
            for (List<android.graphics.Rect> row : rows)
            {
                int dist = Math.abs(averageRectCenterY(row) - r.centerY());
                if (dist <= 90 && dist < bestDist)
                {
                    bestDist = dist;
                    bestRow = row;
                }
            }

            if (bestRow == null)
            {
                bestRow = new ArrayList<>();
                rows.add(bestRow);
            }
            bestRow.add(r);
        }

        return rows.stream()
                .filter(row -> row.size() >= 2)
                .peek(row -> row.sort(Comparator.comparingInt(android.graphics.Rect::centerX)))
                .collect(Collectors.toList());
    }

    private List<android.graphics.Rect> chooseClickBlockChoiceRow(List<List<android.graphics.Rect>> rows, int targetY)
    {
        if (rows == null || rows.isEmpty()) return null;
        return rows.stream()
                .min(Comparator.comparingInt(row -> {
                    int rowY = averageRectCenterY(row);
                    int score = Math.abs(rowY - targetY);
                    if (rowY < targetY - 180) score += 900;
                    if (rowY > targetY + 850) score += 500;
                    return score;
                }))
                .orElse(null);
    }

    private int averageRectCenterY(List<android.graphics.Rect> row)
    {
        if (row == null || row.isEmpty()) return -1;
        int sum = 0;
        for (android.graphics.Rect r : row) sum += r.centerY();
        return sum / row.size();
    }

    private int findClickBlockColumnHeaderX(org.w3c.dom.NodeList nodes, String answerStr, int rowY)
    {
        if (nodes == null || rowY <= 0) return -1;
        String cleanAnswer = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(answerStr));
        if (cleanAnswer.isEmpty()) return -1;

        int bestX = -1;
        int bestScore = Integer.MAX_VALUE;
        boolean answerIsNumeric = cleanAnswer.matches("\\d+");

        for (int i = 0; i < nodes.getLength(); i++)
        {
            org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
            android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
            if (r == null || r.width() <= 0 || r.height() <= 0) continue;
            if (r.centerX() < widthOfScreen * 0.32f) continue;
            if (r.centerY() >= rowY - 45 || r.centerY() <= 180) continue;
            if (r.width() > widthOfScreen * 0.42f || r.height() > 520) continue;

            String raw = (node.getAttribute("text") + " " + node.getAttribute("content-desc")).trim();
            String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(raw));
            if (clean.isEmpty()) continue;

            boolean match;
            if (answerIsNumeric)
            {
                match = clean.equals(cleanAnswer)
                        || clean.startsWith(cleanAnswer + "lan")
                        || clean.startsWith(cleanAnswer + "diem")
                        || clean.startsWith(cleanAnswer + "point")
                        || clean.startsWith(cleanAnswer + "star")
                        || clean.matches("^" + java.util.regex.Pattern.quote(cleanAnswer) + "[a-z].*");
            }
            else
            {
                match = isClickAnswerMatchOcrFriendly(clean, cleanAnswer, true);
            }
            if (!match) continue;

            int score = Math.abs(rowY - r.centerY()) + Math.abs(r.centerX() - xCenter) / 8;
            if (score < bestScore)
            {
                bestScore = score;
                bestX = r.centerX();
            }
        }
        return bestX;
    }

    private int guessClickBlockColumnX(List<android.graphics.Rect> row, String answerStr)
    {
        if (row == null || row.isEmpty()) return -1;
        String cleanAnswer = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(answerStr));
        if (!cleanAnswer.matches("\\d+")) return -1;

        int value;
        try { value = Integer.parseInt(cleanAnswer); }
        catch (Exception e) { return -1; }

        if (row.size() <= 7 && value >= 1 && value <= row.size())
        {
            return row.get(value - 1).centerX();
        }
        if (value >= 0 && value < row.size())
        {
            return row.get(value).centerX();
        }
        return -1;
    }
    private boolean matchesAnswerCandidateOcrFriendly(String node, String answer, boolean allowContains)
    {
        if (HSQTools.equalsOcrFriendly(node, answer)) return true;

        boolean answerIsNumeric = answer.matches("\\d+");
        if (answerIsNumeric && startsWithAnswerTokenOcrFriendly(node, answer)) return true;

        if (allowContains)
        {
            if (answer.length() >= 3 && HSQTools.containsOcrFriendly(node, answer) && node.length() <= answer.length() + 5) return true;
            if (!answerIsNumeric && node.length() >= 5 && HSQTools.containsOcrFriendly(answer, node)) return true;
        }

        int minLen = Math.min(node.length(), answer.length());
        if (minLen >= 4)
        {
            int lenDiff = Math.abs(node.length() - answer.length());
            int allowed = Math.max(1, (int) (minLen * 0.20f));
            return lenDiff <= allowed + 2 && HSQTools.levenshtein(node, answer) <= allowed;
        }

        return false;
    }

    private String stripLeadingRadioGlyph(String text)
    {
        if (text == null || text.length() <= 1) return text;

        char first = text.charAt(0);
        if (first == 'o' || first == '0')
        {
            return text.substring(1);
        }

        return text;
    }

    private boolean startsWithAnswerTokenOcrFriendly(String full, String token)
    {
        if (full == null || token == null || full.length() < token.length()) return false;
        if (!HSQTools.equalsOcrFriendly(full.substring(0, token.length()), token)) return false;
        if (full.length() == token.length()) return true;

        // Target số "2" được ăn "2verybad", nhưng không được ăn nhầm "20" / "21".
        char next = full.charAt(token.length());
        return !token.matches("\\d+") || !Character.isDigit(next);
    }

    private static final int OCR_LEVEL_BLOCK = 0;
    private static final int OCR_LEVEL_LINE = 1;
    private static final int OCR_LEVEL_ELEMENT = 2;

    private static class SmartOcrUnit
    {
        String text;
        android.graphics.Rect rect;
        int level;

        SmartOcrUnit(String text, android.graphics.Rect rect, int level)
        {
            this.text = text;
            this.rect = rect;
            this.level = level;
        }

        int centerX()
        {
            return rect.centerX();
        }

        int centerY()
        {
            return rect.centerY();
        }
    }

    private static class AsblNodeTemp
    {
        String text;
        int cx, cy, left, top, right, bottom;
        String clazz;
        boolean isClickable;
        int fenceLeft;
        int fenceRight;

        AsblNodeTemp(String t, int l, int tp, int r, int b, String clz, boolean click)
        {
            text = t;
            left = l;
            top = tp;
            right = r;
            bottom = b;
            cx = (l + r) / 2;
            cy = (tp + b) / 2;
            clazz = clz;
            isClickable = click;
            fenceLeft = l;
            fenceRight = r;
        }

        int width()
        {
            return Math.max(1, right - left);
        }

        int height()
        {
            return Math.max(1, bottom - top);
        }

        int area()
        {
            return width() * height();
        }
    }

    private static class FusionResult
    {
        String displayText;
        android.graphics.Rect visualRect;
        float score;

        FusionResult(String text, android.graphics.Rect rect, float score)
        {
            this.displayText = text;
            this.visualRect = rect;
            this.score = score;
        }
    }
    private List<HSQTools.TextBlock> getCheckAnswerSmart()
    {
        int tryAgain = 0;

        while (true)
        {
            try
            {
                List<String> blacklist = Arrays.asList("backbutton", "offerwall", "skiptomaincontent");
                List<HSQTools.TextBlock> finalGrid = new ArrayList<>();

                List<AsblNodeTemp> asblList = dedupeAsblNodes(collectAsblTextNodes(blacklist));
                applyRowAwareFences(asblList);

                List<SmartOcrUnit> ocrUnits = collectSmartOcrUnits();
                List<android.graphics.Rect> fusedRects = new ArrayList<>();

                for (AsblNodeTemp asblNode : asblList)
                {
                    FusionResult fused = tryFuseAsblWithOcr(asblNode, ocrUnits, blacklist);
                    if (fused != null)
                    {
                        finalGrid.add(new HSQTools.TextBlock(
                                fused.displayText,
                                fused.visualRect.centerX(),
                                fused.visualRect.centerY()
                        ));
                        fusedRects.add(fused.visualRect);
                    }
                    else if (asblNode.text != null && asblNode.text.trim().matches("^[0-9]+$"))
                    {
                        // Vớt vát ASBL thuần túy nếu nó là 1 con số (VD: nút scale) mà OCR bị mù
                        // Bỏ luôn check isSuspiciousAsblBounds vì nút số nằm ngang màn hình sẽ bị tính là Suspicious (chiếm > 72% chiều ngang)
                        // Không có Ghost Node nào mà nội dung chỉ là 1 con số duy nhất cả!
                        finalGrid.add(new HSQTools.TextBlock(
                                asblNode.text.trim(),
                                asblNode.cx,
                                asblNode.cy
                        ));
                    }
                }

                List<HSQTools.TextBlock> fallbackOcr = buildOcrFallbackTextBlocks(ocrUnits, fusedRects, blacklist);
                for (HSQTools.TextBlock ocrNode : fallbackOcr)
                {
                    if (!isNearDuplicate(finalGrid, ocrNode))
                    {
                        finalGrid.add(ocrNode);
                    }
                }

                finalGrid.sort((node1, node2) ->
                {
                    if (Math.abs(node1.y - node2.y) <= 15)
                    {
                        return Integer.compare(node1.x, node2.x);
                    }
                    return Integer.compare(node1.y, node2.y);
                });

                finalGrid = finalGrid.stream()
                        .filter(x -> x.y > 180)
                        .collect(Collectors.toList());
                // =========================================================================
                // 🔥 THUẬT TOÁN GHÉP CÂU OCR THÔNG MINH (CHỈ KÍCH HOẠT KHI ASBL MÙ TỊT)
                // =========================================================================
                if (asblList.isEmpty() && !finalGrid.isEmpty()) {
                    List<HSQTools.TextBlock> groupedGrid = new ArrayList<>();
                    HSQTools.TextBlock currentGroup = null;
                    int lastY = 0;
                    int lastX = 0;
                    for (HSQTools.TextBlock tb : finalGrid) {
                        if (currentGroup == null) {
                            currentGroup = new HSQTools.TextBlock(tb.text, tb.x, tb.y);
                            lastY = tb.y;
                            lastX = tb.x;
                            continue;
                        }
                        int deltaY = Math.abs(tb.y - lastY);
                        int deltaX = Math.abs(tb.x - lastX);

                        boolean isSameRow = deltaY <= 20;
                        boolean isNextLine = deltaY > 20 && deltaY <= 90;
                        boolean isTooFarY = deltaY > 90;

                        // 1. CHỐNG MA TRẬN NGANG: Cùng 1 dòng nhưng X cách xa > 150px -> Đây là 2 cột ngang -> CẮT!
                        boolean isGridColumn = isSameRow && deltaX > 150;

                        // 2. LỆCH LỀ TRÁI: Dòng tiếp theo nhưng lề trái lệch nhau > 400px -> CẮT!
                        boolean isTooFarX = isNextLine && deltaX > 400;
                        String currentTextClean = currentGroup.text.trim();
                        // 3. CHỐT CÂU: Gặp dấu chấm, hỏi, chấm than, hai chấm ở cuối -> CẮT!
                        boolean hasPunctuation = currentTextClean.matches(".*[?.!:]$");

                        // 4. CHỐNG GHÉP NHẦM ĐÁP ÁN DỌC (OPTION STACK BUSTER):
                        // - Nếu dòng trên ngắn (< 35 ký tự, tức là không bị tràn màn hình)
                        // - VÀ dòng hiện tại KHÔNG BẮT ĐẦU bằng chữ thường (VD: Bắt đầu bằng chữ Hoa, hoặc Số)
                        // -> Đích thị là một danh sách đáp án xếp dọc -> CẮT NGAY VÀ LUÔN!
                        boolean isPreviousLineShort = currentTextClean.length() < 35;
                        boolean startsWithLowerCase = !tb.text.isEmpty() && Character.isLowerCase(tb.text.charAt(0));
                        boolean isOptionStack = isNextLine && isPreviousLineShort && !startsWithLowerCase;
                        if (isTooFarY || isTooFarX || isGridColumn || hasPunctuation || isOptionStack) {
                            // Chốt sổ nhóm cũ
                            groupedGrid.add(currentGroup);
                            // Khởi tạo nhóm mới
                            currentGroup = new HSQTools.TextBlock(tb.text, tb.x, tb.y);
                            lastY = tb.y;
                            lastX = tb.x;
                        } else {
                            // Cùng thuộc 1 câu -> Nối Text
                            currentGroup.text = currentGroup.text + " " + tb.text.trim();
                            lastY = tb.y;
                            lastX = tb.x;
                        }
                    }

                    if (currentGroup != null) {
                        groupedGrid.add(currentGroup);
                    }
                    finalGrid = groupedGrid;
                }
                // =========================================================================
                if (finalGrid.isEmpty())
                {
                    if (tryAgain == 0)
                    {
                        swipe(xs, yBot, xs, yTop, 2000);
                        delay(2000);
                        swipe(xs, yTop, xs, yBot, 2000);
                        delay(5000);
                        tryAgain++;
                        continue;
                    }
                    else
                    {
                        finalGrid = HSQTools.getOcrTextBlocks().stream()
                                .filter(x -> x.y > 180)
                                .collect(Collectors.toList());
                    }
                }

                return finalGrid;
            }
            catch (Exception e)
            {
                delay(5000);
            }
        }
    }
    private List<AsblNodeTemp> collectAsblTextNodes(List<String> blacklist)
    {
        List<AsblNodeTemp> result = new ArrayList<>();
        AccessibilityNodeInfo root = ASBLBridgeService.asblService.getRootInActiveWindow();
        if (root == null) return result;

        try
        {
            root.refresh();
            List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
            HSQTools.getAllNodesRec(root, allNodes);

            for (AccessibilityNodeInfo node : allNodes)
            {
                if (node == null || !node.isVisibleToUser()) continue;

                CharSequence nodeText = node.getText();
                if (nodeText == null || nodeText.toString().trim().isEmpty())
                    nodeText = node.getContentDescription();

                if (nodeText == null) continue;

                String rawText = nodeText.toString()
                        .replace("\n", " ")
                        .replaceAll("\\s+", " ")
                        .trim();

                if (rawText.isEmpty()) continue;

                String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(rawText));
                if (clean.isEmpty() || blacklist.contains(clean)) continue;

                android.graphics.Rect bounds = new android.graphics.Rect();
                node.getBoundsInScreen(bounds);

                if (bounds.width() <= 5 || bounds.height() <= 5) continue;
                if (bounds.centerY() <= 180 || bounds.centerY() >= heightOfScreen - 40) continue;
                if (bounds.height() > 650) continue;

                result.add(new AsblNodeTemp(
                        rawText,
                        bounds.left,
                        bounds.top,
                        bounds.right,
                        bounds.bottom,
                        node.getClassName() != null ? node.getClassName().toString() : "",
                        node.isClickable()
                ));
            }
        }
        catch (Exception ignored)
        {
        }
        finally
        {
            root.recycle();
        }

        return result;
    }

    private List<AsblNodeTemp> dedupeAsblNodes(List<AsblNodeTemp> rawNodes)
    {
        List<AsblNodeTemp> result = new ArrayList<>();

        rawNodes.sort(Comparator
                .comparingInt((AsblNodeTemp n) -> n.cy)
                .thenComparingInt(n -> n.cx)
                .thenComparingInt(AsblNodeTemp::area));

        for (AsblNodeTemp node : rawNodes)
        {
            String cleanNode = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(node.text));
            AsblNodeTemp duplicate = null;

            for (AsblNodeTemp existing : result)
            {
                String cleanExisting = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(existing.text));

                boolean sameText =
                        cleanExisting.equals(cleanNode) ||
                                (cleanExisting.length() >= 5 && cleanNode.length() >= 5 &&
                                        (HSQTools.containsOcrFriendly(cleanExisting, cleanNode) ||
                                                HSQTools.containsOcrFriendly(cleanNode, cleanExisting)));

                boolean nearSameSpot =
                        Math.abs(existing.cy - node.cy) <= 45 &&
                                Math.abs(existing.cx - node.cx) <= Math.max(120, Math.min(existing.width(), node.width()));

                if (sameText && nearSameSpot)
                {
                    duplicate = existing;
                    break;
                }
            }

            if (duplicate == null)
            {
                result.add(node);
                continue;
            }

            boolean currentBetter = node.area() < duplicate.area();
            if (!currentBetter && node.area() == duplicate.area())
            {
                currentBetter = node.width() < duplicate.width();
            }

            if (currentBetter)
            {
                result.remove(duplicate);
                result.add(node);
            }
        }

        return result;
    }

    private void applyRowAwareFences(List<AsblNodeTemp> nodes)
    {
        List<AsblNodeTemp> sorted = new ArrayList<>(nodes);
        sorted.sort(Comparator
                .comparingInt((AsblNodeTemp n) -> n.cy)
                .thenComparingInt(n -> n.cx));

        List<List<AsblNodeTemp>> rows = new ArrayList<>();

        for (AsblNodeTemp node : sorted)
        {
            List<AsblNodeTemp> row = rows.isEmpty() ? null : rows.get(rows.size() - 1);

            if (row == null)
            {
                row = new ArrayList<>();
                rows.add(row);
            }
            else
            {
                int anchorY = row.get(0).cy;
                int tolerance = Math.max(55, Math.min(95, Math.min(row.get(0).height(), node.height())));
                if (Math.abs(anchorY - node.cy) > tolerance)
                {
                    row = new ArrayList<>();
                    rows.add(row);
                }
            }

            row.add(node);
        }

        for (List<AsblNodeTemp> row : rows)
        {
            row.sort(Comparator.comparingInt(n -> n.cx));

            for (int i = 0; i < row.size(); i++)
            {
                AsblNodeTemp cur = row.get(i);

                int leftFence = Math.max(0, cur.left - 25);
                int rightFence = Math.min(widthOfScreen, cur.right + 25);

                int prevMid = (i > 0) ? (row.get(i - 1).cx + cur.cx) / 2 : leftFence;
                int nextMid = (i < row.size() - 1) ? (cur.cx + row.get(i + 1).cx) / 2 : rightFence;

                if (row.size() > 1)
                {
                    leftFence = Math.max(leftFence, prevMid - 18);
                    rightFence = Math.min(rightFence, nextMid + 18);

                    if (isSuspiciousAsblBounds(cur))
                    {
                        leftFence = Math.max(0, prevMid - 8);
                        rightFence = Math.min(widthOfScreen, nextMid + 8);
                    }
                }

                if (rightFence - leftFence < 60)
                {
                    leftFence = Math.max(0, cur.cx - 120);
                    rightFence = Math.min(widthOfScreen, cur.cx + 120);
                }

                cur.fenceLeft = leftFence;
                cur.fenceRight = rightFence;
            }
        }
    }

    private List<SmartOcrUnit> collectSmartOcrUnits()
    {
        List<SmartOcrUnit> list = new ArrayList<>();
        android.graphics.Bitmap screenshot = HSQTools.getScreenBitmap();
        if (screenshot == null) return list;

        try
        {
            InputImage image = InputImage.fromBitmap(screenshot, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            Text result = Tasks.await(recognizer.process(image));

            if (result != null && result.getTextBlocks() != null)
            {
                for (Text.TextBlock block : result.getTextBlocks())
                {
                    addSmartOcrUnit(list, block.getText(), block.getBoundingBox(), OCR_LEVEL_BLOCK);

                    for (Text.Line line : block.getLines())
                    {
                        addSmartOcrUnit(list, line.getText(), line.getBoundingBox(), OCR_LEVEL_LINE);

                        for (Text.Element element : line.getElements())
                        {
                            addSmartOcrUnit(list, element.getText(), element.getBoundingBox(), OCR_LEVEL_ELEMENT);
                        }
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
        finally
        {
            screenshot.recycle();
        }

        list.sort(Comparator
                .comparingInt(SmartOcrUnit::centerY)
                .thenComparingInt(SmartOcrUnit::centerX)
                .thenComparingInt(u -> u.level));

        return list;
    }

    private void addSmartOcrUnit(List<SmartOcrUnit> list, String text, android.graphics.Rect rect, int level)
    {
        if (rect == null || text == null) return;

        String cleaned = text.replace("\n", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) return;
        if (rect.width() <= 4 || rect.height() <= 4) return;
        if (rect.centerY() <= 180 || rect.centerY() >= heightOfScreen - 40) return;

        list.add(new SmartOcrUnit(cleaned, new android.graphics.Rect(rect), level));
    }

    private FusionResult tryFuseAsblWithOcr(AsblNodeTemp node, List<SmartOcrUnit> ocrUnits, List<String> blacklist)
    {
        String cleanAsbl = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(node.text));
        if (cleanAsbl.isEmpty() || blacklist.contains(cleanAsbl)) return null;

        List<SmartOcrUnit> scoped = collectScopedVisualUnits(node, ocrUnits);
        if (scoped.isEmpty()) return null;

        FusionResult best = null;

        List<SmartOcrUnit> elements = scoped.stream()
                .filter(u -> u.level == OCR_LEVEL_ELEMENT)
                .collect(Collectors.toList());

        if (elements.size() >= 2)
        {
            best = pickBetterFusion(best, buildFusionFromUnits(node, elements));
        }

        List<SmartOcrUnit> lines = scoped.stream()
                .filter(u -> u.level == OCR_LEVEL_LINE)
                .collect(Collectors.toList());

        if (!lines.isEmpty())
        {
            best = pickBetterFusion(best, buildFusionFromUnits(node, lines));
        }

        for (SmartOcrUnit unit : scoped)
        {
            if (unit.level == OCR_LEVEL_ELEMENT) continue;
            best = pickBetterFusion(best, buildFusionFromUnits(node, java.util.Collections.singletonList(unit)));
        }

        float minScore = isSuspiciousAsblBounds(node) ? 1.55f : 1.20f;
        return (best != null && best.score >= minScore) ? best : null;
    }

    private List<SmartOcrUnit> collectScopedVisualUnits(AsblNodeTemp node, List<SmartOcrUnit> ocrUnits)
    {
        List<SmartOcrUnit> result = new ArrayList<>();

        int fenceWidth = Math.max(1, node.fenceRight - node.fenceLeft);
        int halfBand = isSuspiciousAsblBounds(node) ? 120 : Math.min(170, Math.max(80, node.height() * 2));
        int topBand = Math.max(0, node.cy - halfBand);
        int bottomBand = Math.min(heightOfScreen, node.cy + halfBand);

        boolean hasTightFence = (node.fenceRight - node.fenceLeft) < (node.width() - 40);

        for (SmartOcrUnit unit : ocrUnits)
        {
            boolean yFit = unit.rect.bottom >= topBand && unit.rect.top <= bottomBand;
            if (!yFit) continue;

            int overlap = horizontalOverlap(unit.rect, node.fenceLeft, node.fenceRight);
            boolean xFit =
                    overlap >= Math.max(8, Math.min(unit.rect.width(), fenceWidth) / 4) ||
                            (unit.centerX() >= node.fenceLeft - 35 && unit.centerX() <= node.fenceRight + 35);

            if (!xFit) continue;

            if (unit.level == OCR_LEVEL_BLOCK && hasTightFence && unit.rect.width() > (fenceWidth * 1.75f))
                continue;

            result.add(unit);
        }

        result.sort(Comparator
                .comparingInt(SmartOcrUnit::centerY)
                .thenComparingInt(SmartOcrUnit::centerX)
                .thenComparingInt(u -> u.level));

        return result;
    }

    private FusionResult buildFusionFromUnits(AsblNodeTemp node, List<SmartOcrUnit> units)
    {
        if (units == null || units.isEmpty()) return null;

        List<SmartOcrUnit> ordered = new ArrayList<>(units);
        ordered.sort(Comparator
                .comparingInt(SmartOcrUnit::centerY)
                .thenComparingInt(SmartOcrUnit::centerX)
                .thenComparingInt(u -> u.level));

        String visualText = composeVisualText(ordered);
        android.graphics.Rect visualRect = unionRect(ordered);

        if (visualRect == null || visualText.isEmpty()) return null;

        float score = scoreAsblAgainstVisual(node, visualText, visualRect, ordered);
        if (score <= 0f) return null;

        return new FusionResult(chooseDisplayText(node.text, visualText), visualRect, score);
    }

    private String composeVisualText(List<SmartOcrUnit> units)
    {
        if (units == null || units.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int lastY = Integer.MIN_VALUE;

        for (SmartOcrUnit unit : units)
        {
            String text = unit.text == null ? "" : unit.text.replace("\n", " ").replaceAll("\\s+", " ").trim();
            if (text.isEmpty()) continue;

            if (sb.length() > 0)
            {
                sb.append(" ");
            }

            sb.append(text);
            lastY = unit.centerY();
        }

        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private android.graphics.Rect unionRect(List<SmartOcrUnit> units)
    {
        android.graphics.Rect result = null;

        for (SmartOcrUnit unit : units)
        {
            if (result == null)
            {
                result = new android.graphics.Rect(unit.rect);
            }
            else
            {
                result.union(unit.rect);
            }
        }

        return result;
    }

    private float scoreAsblAgainstVisual(AsblNodeTemp node, String visualText, android.graphics.Rect visualRect, List<SmartOcrUnit> units)
    {
        String cleanAsbl = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(node.text));
        String cleanVisual = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(visualText));

        if (cleanAsbl.isEmpty() || cleanVisual.isEmpty()) return 0f;

        float compactScore = scoreCompactSimilarity(cleanAsbl, cleanVisual);
        float tokenScore = scoreTokenCoverage(node.text, visualText);

        int fenceWidth = Math.max(1, node.fenceRight - node.fenceLeft);
        float penalty = 0f;

        if (visualRect.left < node.fenceLeft - 30) penalty += 0.18f;
        if (visualRect.right > node.fenceRight + 30) penalty += 0.18f;
        if (visualRect.width() > fenceWidth * 1.25f) penalty += 0.28f;
        if (visualRect.height() > Math.max(170, node.height() * 2)) penalty += 0.20f;

        boolean hasFineGrain = false;
        for (SmartOcrUnit unit : units)
        {
            if (unit.level != OCR_LEVEL_BLOCK)
            {
                hasFineGrain = true;
                break;
            }
        }

        float bonus = hasFineGrain ? 0.12f : 0f;
        if (compactScore >= 0.99f) bonus += 0.35f;
        else if (compactScore >= 0.88f) bonus += 0.22f;
        else if (compactScore >= 0.75f) bonus += 0.10f;

        return (compactScore * 1.35f) + tokenScore + bonus - penalty;
    }

    private float scoreCompactSimilarity(String a, String b)
    {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0f;

        if (HSQTools.equalsOcrFriendly(a, b)) return 1f;

        if (HSQTools.containsOcrFriendly(a, b) || HSQTools.containsOcrFriendly(b, a))
        {
            float shorterRatio = (float) Math.min(a.length(), b.length()) / Math.max(a.length(), b.length());
            return 0.82f + (0.18f * shorterRatio);
        }

        int dist = HSQTools.levenshtein(a, b);
        return Math.max(0f, 1f - ((float) dist / Math.max(a.length(), b.length())));
    }

    private float scoreTokenCoverage(String source, String observed)
    {
        String normSource = normalizeTokenText(source);
        String normObserved = normalizeTokenText(observed);

        if (normSource.isEmpty() || normObserved.isEmpty()) return 0f;

        String[] srcTokens = normSource.split(" ");
        String[] obsTokens = normObserved.split(" ");

        int total = 0;
        int hits = 0;

        for (String src : srcTokens)
        {
            if (src == null || src.isEmpty()) continue;
            total++;

            for (String obs : obsTokens)
            {
                if (obs == null || obs.isEmpty()) continue;

                if (src.equals(obs))
                {
                    hits++;
                    break;
                }

                if (src.length() >= 5 && obs.length() >= 5 && HSQTools.levenshtein(src, obs) <= 1)
                {
                    hits++;
                    break;
                }
            }
        }

        return total == 0 ? 0f : (float) hits / total;
    }

    private String normalizeTokenText(String raw)
    {
        if (raw == null) return "";

        String temp = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD);
        temp = temp.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        temp = temp.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
        temp = temp.replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");

        return temp;
    }

    private String chooseDisplayText(String asblText, String visualText)
    {
        String cleanAsblDisplay = asblText == null ? "" : asblText.replace("\n", " ").replaceAll("\\s+", " ").trim();
        String cleanVisualDisplay = visualText == null ? "" : visualText.replace("\n", " ").replaceAll("\\s+", " ").trim();

        if (cleanAsblDisplay.isEmpty()) return cleanVisualDisplay;
        if (cleanVisualDisplay.isEmpty()) return cleanAsblDisplay;

        if (scoreTokenCoverage(cleanAsblDisplay, cleanVisualDisplay) >= 0.60f &&
                cleanAsblDisplay.length() >= cleanVisualDisplay.length())
        {
            return cleanAsblDisplay;
        }

        return cleanVisualDisplay;
    }

    private FusionResult pickBetterFusion(FusionResult current, FusionResult candidate)
    {
        if (candidate == null) return current;
        if (current == null) return candidate;

        if (candidate.score > current.score) return candidate;

        if (Math.abs(candidate.score - current.score) < 0.0001f &&
                candidate.visualRect.height() < current.visualRect.height())
        {
            return candidate;
        }

        return current;
    }

    private List<HSQTools.TextBlock> buildOcrFallbackTextBlocks(List<SmartOcrUnit> ocrUnits, List<android.graphics.Rect> fusedRects, List<String> blacklist)
    {
        List<HSQTools.TextBlock> result = new ArrayList<>();

        List<SmartOcrUnit> lines = ocrUnits.stream()
                .filter(u -> u.level == OCR_LEVEL_LINE)
                .sorted(Comparator.comparingInt(SmartOcrUnit::centerY).thenComparingInt(SmartOcrUnit::centerX))
                .collect(Collectors.toList());

        for (SmartOcrUnit line : lines)
        {
            if (!isUsableFallbackUnit(line, fusedRects, blacklist)) continue;

            HSQTools.TextBlock candidate = new HSQTools.TextBlock(line.text, line.centerX(), line.centerY());
            if (!isNearDuplicate(result, candidate))
            {
                result.add(candidate);
            }
        }

        List<SmartOcrUnit> blocks = ocrUnits.stream()
                .filter(u -> u.level == OCR_LEVEL_BLOCK)
                .sorted(Comparator.comparingInt(SmartOcrUnit::centerY).thenComparingInt(SmartOcrUnit::centerX))
                .collect(Collectors.toList());

        for (SmartOcrUnit block : blocks)
        {
            if (!isUsableFallbackUnit(block, fusedRects, blacklist)) continue;

            boolean lineAlreadyCovers = false;
            for (SmartOcrUnit line : lines)
            {
                if (rectOverlapRatio(block.rect, line.rect) >= 0.45f)
                {
                    lineAlreadyCovers = true;
                    break;
                }
            }

            if (lineAlreadyCovers) continue;

            HSQTools.TextBlock candidate = new HSQTools.TextBlock(block.text, block.centerX(), block.centerY());
            if (!isNearDuplicate(result, candidate))
            {
                result.add(candidate);
            }
        }

        return result;
    }

    private boolean isUsableFallbackUnit(SmartOcrUnit unit, List<android.graphics.Rect> fusedRects, List<String> blacklist)
    {
        String clean = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(unit.text));
        if (clean.isEmpty() || blacklist.contains(clean)) return false;

        for (android.graphics.Rect fused : fusedRects)
        {
            if (rectOverlapRatio(unit.rect, fused) >= 0.55f)
            {
                return false;
            }
        }

        return true;
    }

    private boolean isNearDuplicate(List<HSQTools.TextBlock> list, HSQTools.TextBlock candidate)
    {
        String cleanCandidate = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(candidate.text));

        for (HSQTools.TextBlock existing : list)
        {
            String cleanExisting = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(existing.text));

            boolean sameSpot =
                    Math.abs(existing.x - candidate.x) <= 140 &&
                            Math.abs(existing.y - candidate.y) <= 45;

            boolean sameText =
                    cleanExisting.equals(cleanCandidate) ||
                            (cleanCandidate.length() >= 5 && cleanExisting.length() >= 5 &&
                                    (HSQTools.containsOcrFriendly(cleanExisting, cleanCandidate) ||
                                            HSQTools.containsOcrFriendly(cleanCandidate, cleanExisting)));

            if (sameSpot && sameText)
            {
                return true;
            }
        }

        return false;
    }

    private boolean isSuspiciousAsblBounds(AsblNodeTemp node)
    {
        return node.left <= 0 ||
                node.right >= widthOfScreen - 1 ||
                node.width() > (widthOfScreen * 0.72f) ||
                node.height() > 220;
    }

    private int horizontalOverlap(android.graphics.Rect rect, int left, int right)
    {
        return Math.max(0, Math.min(rect.right, right) - Math.max(rect.left, left));
    }

    private float rectOverlapRatio(android.graphics.Rect a, android.graphics.Rect b)
    {
        int interW = Math.max(0, Math.min(a.right, b.right) - Math.max(a.left, b.left));
        int interH = Math.max(0, Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top));
        int interArea = interW * interH;

        if (interArea <= 0) return 0f;

        int minArea = Math.min(Math.max(1, a.width() * a.height()), Math.max(1, b.width() * b.height()));
        return (float) interArea / minArea;
    }
    private android.graphics.Rect findInputRectByXmlLoose(String xml, String labelToFind)
    {
        try
        {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

            String normTarget = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(labelToFind));
            int labelCenterY = -1;
            int minLabelHeight = Integer.MAX_VALUE;

            for (int i = 0; i < nodes.getLength(); i++)
            {
                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                String clazz = node.getAttribute("class");
                if (clazz.contains("EditText") || clazz.contains("Button") || clazz.contains("WebView")) continue;

                String combined = HSQTools.getOnlyTextLinq(HSQTools.normalizeText(node.getAttribute("text") + " " + node.getAttribute("content-desc")));
                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                if (r == null || combined.isEmpty() || normTarget.length() < 2) continue;
                if (r.height() <= 10 || r.height() >= (heightOfScreen * 0.8)) continue;
                if (r.centerY() <= 180 || r.centerY() >= heightOfScreen - 50) continue;

                boolean labelMatch =
                        combined.equals(normTarget) ||
                                combined.contains(normTarget) ||
                                (combined.length() >= 4 && normTarget.contains(combined)) ||
                                (normTarget.length() >= 4 && HSQTools.containsOcrFriendly(combined, normTarget));

                if (labelMatch && r.height() < minLabelHeight)
                {
                    minLabelHeight = r.height();
                    labelCenterY = r.centerY();
                }
            }

            // 🔥 BỌC THÉP TỐI THƯỢNG: Nếu CÓ truyền Mỏ neo mà KHÔNG TÌM THẤY mỏ neo -> Trả về null để ÉP Lệnh VUỐT!
            // Không được đoán mò chọn đại ô input khác trên màn hình!
            if (normTarget.length() >= 2 && labelCenterY == -1) {
                return null;
            }

            android.graphics.Rect onlyInput = null;
            android.graphics.Rect bestInput = null;
            int visibleInputCount = 0;
            int bestScore = Integer.MAX_VALUE;

            for (int i = 0; i < nodes.getLength(); i++)
            {
                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                String clazz = node.getAttribute("class");
                String rid = node.getAttribute("resource-id").toLowerCase();

                boolean isInput =
                        clazz.contains("EditText") ||
                                clazz.contains("AutoCompleteTextView") ||
                                rid.contains("answer") ||
                                rid.contains("input") ||
                                rid.contains("edit");

                if (!isInput) continue;

                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                if (r == null || r.width() < 80 || r.height() < 20) continue;
                if (r.centerY() <= 180 || r.centerY() >= heightOfScreen - 80) continue;

                visibleInputCount++;
                onlyInput = r;

                int score = 0;

                if ("true".equals(node.getAttribute("focused"))) score -= 10000;
                if ("true".equals(node.getAttribute("focusable"))) score -= 1000;
                if ("true".equals(node.getAttribute("clickable"))) score -= 500;
                if (rid.contains("answer")) score -= 3000;

                if (labelCenterY != -1)
                {
                    int dist = Math.abs(r.centerY() - labelCenterY);
                    if (dist > 800) continue; // Bỏ qua nếu cách mỏ neo quá xa (800px)
                    score += dist;
                }
                else
                {
                    score += 5000;
                }

                if (score < bestScore)
                {
                    bestScore = score;
                    bestInput = r;
                }
            }

            if (bestInput != null) return bestInput;
            if (visibleInputCount == 1) return onlyInput;
        }
        catch (Exception ignored)
        {
        }

        return null;
    }
    private IApiHelper createAIHelper() {
        while(true)
        {
            if (AIProxyEnabled)
            {
                return new ServerQueuedApiHelper(
                        HSQConfig.getContext(),
                        apiRun,
                        AIProxyUrl,
                        AIWebSite,
                        AIApiKey,
                        aiModel,
                        deviceID,
                        false
                );
            }

            String webSiteLower = AIWebSite.toLowerCase();
            if (webSiteLower.contains("nexusmmo"))
            {
                return new NexusMmoApiHelper(HSQConfig.getContext(), AIApiKey, aiModel, false);
            }
            else if (webSiteLower.contains("ai-box"))
            {
                return new AiBoxApiHelper(HSQConfig.getContext(), AIApiKey, aiModel, false);
            }
            else if (webSiteLower.contains("tokenrouter"))
            {
                return new TokenRouterApiHelper(HSQConfig.getContext(), AIApiKey, aiModel, false);
            }
            else if (webSiteLower.contains("htmustc"))
            {
                return new HTMustcApiHelper(HSQConfig.getContext(), AIApiKey, aiModel, false);
            }
            updateNotificationContent("Sai mô hình AI");
            delay(120000);
        }
    }
}







