package com.quayquay.shtools;

import static com.quayquay.shtools.extention.ASUtils.delay;
import static com.quayquay.shtools.services.ASBLBridgeService.clearrecents;
import static com.quayquay.shtools.services.ASBLBridgeService.findAndClickByTextDes;
import static com.quayquay.shtools.services.ASBLBridgeService.globalBack;
import static com.quayquay.shtools.services.ASBLBridgeService.globalHome;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.quayquay.hsq.tools.CompactAIHelper;
import com.quayquay.hsq.tools.HSQConfig;
import com.quayquay.hsq.tools.HSQDevice;
import com.quayquay.hsq.tools.HSQFacebook;
import com.quayquay.hsq.tools.HSQFileHelper;
import com.quayquay.hsq.tools.HSQHttps;
import com.quayquay.hsq.tools.HSQRoot;
import com.quayquay.hsq.tools.HSQService;
import com.quayquay.hsq.tools.HSQTools;
import com.quayquay.hsq.tools.HSQTools.TextBlock;
import com.quayquay.hsq.tools.IProfileProvider;
import com.quayquay.hsq.tools.ZoneTokenApiHelper;
import com.quayquay.shtools.extention.AppInstaller;
import com.quayquay.shtools.screendefinitions.ScreenNode;
import com.quayquay.shtools.services.ASBLBridgeService;

import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

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
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StartAuto extends HSQService
{
    private static final String apiAdminServer = BuildConfig.API_ADMIN_SERVER;
    private String apiRun = "";
    //private GeminiApiHelper geminiAI;
    private ZoneTokenApiHelper geminiAI;
    private static final int widthOfScreen = ASBLBridgeService.widthOfScreen;
    //private static final int xLeft = ASBLBridgeService.xLeft;
    //private static final int xRight = ASBLBridgeService.xRight;
    private static final int xCenter = ASBLBridgeService.xCenter;
    private static final int heightOfScreen = ASBLBridgeService.heightOfScreen;
    private static final int yTop = ASBLBridgeService.yTop;
    private static final int yBot = ASBLBridgeService.yBot;
    private static final int yCenter = ASBLBridgeService.yCenter;
    //private static final int dpi = ASBLBridgeService.dpi;
    static AccessibilityService asblService = ASBLBridgeService.asblService;
    private static TextRecognizer textRecognizer;
    private static int apkVersion = 0;
    private static int remotePromtVersion = 0;
    private String localServerIp = "", apiZoneToken = "", idTelegram = "", customAgentRule = "", aiModel = "";

    private static final int VCode = BuildConfig.VERSION_CODE;
    public static final String deviceID = HSQTools.getDeviceSerial(HSQConfig.getContext());
    private static final String shortDeviceID = getShortDeviceID();
    @SuppressLint("SdCardPath")
    private final String imagePath = "/sdcard/Pictures/ImageChat";
    private String pathInfoProfileSaved;

    // Hàm phụ trợ để kiểm tra và lấy 8 ký tự cuối
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

    // Nâng cấp mấy thằng này lên tầm Class để hàm nào cũng xài được
    private int AINguL = 0;
    private int createAgain = 0;
    private int currentState = 0;
    private List<HSQTools.TextBlock> screenBegin = new ArrayList<>();
    private String textAnswer = "";
    private int lastImageCount = 0;
    private static final int STATE_START = 0;
    private static final int STATE_GET_ANSWER = 1;
    private static final int STATE_ANSWER_OK = 2;
    private static final int STATE_ROLLBACK1 = 3;
    String lastScreenHash = ""; // Lưu trữ "Dấu vân tay" của màn hình trước đó
    int stuckCounter = 0;       // Đếm số lần màn hình bị kẹt (không thay đổi)
    final int MAX_STUCK_LIMIT = 2; // Giới hạn số lần kẹt tối đa trước khi báo tử
    public static boolean isStop = false;
    @SuppressLint("SdCardPath")

    @Override
    public void onStarted(JSONObject object)
    {
        if (object != null && object.has("api_key"))
        {
            apiRun = object.optString("api_key", "");
        }
        try
        {
            HSQFileHelper.createFolder(deviceID, imagePath);
            //Truyền service ASBL của sếp cho thư viện
            com.quayquay.hsq.tools.HSQConfig.setASBLService(ASBLBridgeService.asblService);
            //CẮM DÂY CLICK: Chuyền tọa độ từ Thư viện sang cho hàm click của sếp tự múa!
            com.quayquay.hsq.tools.HSQConfig.setASBLBridge(ASBLBridgeService::do_click);
            HSQTools.setIsRooted(false);
            HSQTools.setIsAdminApp(true);
            String directionPath = "/sdcard/Servey/direction.json";
            pathInfoProfileSaved = "/sdcard/Servey/sv_" + deviceID + ".json";
            RegistrationInfo InfoProfile = new RegistrationInfo();
            IProfileProvider profileProvider = null;
            String profileData = "";
            Instant startTime = Instant.now();
            CompactAIHelper.initAI(HSQConfig.getContext());
            startTool:
            while (true)
            {
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
                        updateProfile();
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
                                final RegistrationInfo finalInfoProfile = InfoProfile;

                                int minIncome = 900;
                                int maxIncome = 2000;
                                final int annualHouseholdIncome = (new Random().nextInt((maxIncome - minIncome) + 1) + minIncome) * 1000000; // Đơn vị: VNĐ

                                // Luật: Thu nhập hộ gia đình hàng tháng = Năm / 12
                                final int monthlyHouseholdIncome = annualHouseholdIncome / 12;

                                // Luật: Thu nhập cá nhân hàng tháng = 45% - 60% của Hộ gia đình [cite: 46]
                                int randomPercent = new Random().nextInt(16) + 45; // 45 đến 60
                                final int monthlyPersonalIncome = (monthlyHouseholdIncome * randomPercent) / 100;

                                profileProvider = new IProfileProvider()
                                {
                                    @Override
                                    public String getGenderStr()
                                    {
                                        if (finalInfoProfile.getGender() == RegistrationInfo.Gender.FEMALE)
                                            return "Nữ";
                                        if (finalInfoProfile.getGender() == RegistrationInfo.Gender.MALE)
                                            return "Nam";
                                        return "";
                                    }

                                    @Override
                                    public int getAge()
                                    {
                                        return finalInfoProfile.getAge();
                                    }

                                    @Override
                                    public int getYearOfBirth()
                                    {
                                        return finalInfoProfile.getYearOfBirth();
                                    }

                                    @Override
                                    public String getProvince()
                                    {
                                        return finalInfoProfile.getProvince();
                                    }

                                    @Override
                                    public String getZipCode()
                                    {
                                        return String.valueOf(finalInfoProfile.getZipCode());
                                    }

                                    @Override
                                    public String getEducation()
                                    {
                                        return finalInfoProfile.getEducation();
                                    }

                                    @Override
                                    public String getJobField()
                                    {
                                        return finalInfoProfile.getLinhVucNghe();
                                    }

                                    @Override
                                    public String getJobTitle()
                                    {
                                        return finalInfoProfile.getChucDanh();
                                    }

                                    @Override
                                    public int getChildrenCount()
                                    {
                                        return finalInfoProfile.getChildrenCount();
                                    }

                                    // Ép kiểu chuỗi số (Gửi số gốc để clicktotext dễ fuzzy match, hoặc API xử lý)
                                    @Override
                                    public String getAnnualHouseholdIncome()
                                    {
                                        return String.valueOf(annualHouseholdIncome);
                                    }

                                    @Override
                                    public String getMonthlyHouseholdIncome()
                                    {
                                        return String.valueOf(monthlyHouseholdIncome);
                                    }

                                    @Override
                                    public String getMonthlyPersonalIncome()
                                    {
                                        return String.valueOf(monthlyPersonalIncome);
                                    }
                                };
                                break;
                            }
                            else
                            {
                                delay(30000);
                                updateContent("Thiếu file Info");
                            }
                        }
                        catch (Exception ignore)
                        {
                            delay(30000);
                            updateContent("Lỗi đọc InfoProfile");
                        }
                    }

                    customAgentRule = baseRule + "\n\nĐÂY LÀ THÔNG TIN CÁ NHÂN CỦA BẠN (HÃY BÁM SÁT VÀO ĐÂY ĐỂ TRẢ LỜI KHẢO SÁT):\n" + profileData;
                }
                if(geminiAI == null)
                {
                    geminiAI = new ZoneTokenApiHelper(HSQConfig.getContext(), apiZoneToken, aiModel, false);
                }

                hide();
                updateNotificationContent("Start...");

                List<HSQTools.TextBlock> lastScreen;
                List<HSQTools.TextBlock> AllPointsOK = new ArrayList<>();
                Map<String, Integer> matrixColumnCache = new HashMap<>();

                delay(1000);
                beginApp:
                while (true)
                {
                    int LastInterFace = 0, screenSwipe = 0, xs = xCenter, ysTop = yTop, ysBot = yBot, tempSwipeCount = 0, clickChoose = 0, scanFull, checkloi = 0,
                            swipeDuration = 1500, timeCheckServey = 0;
                    String previousText = "", tempTextAnswer = "";
                    boolean daClick, screenDif, dropDownOpen;
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
                        if (HSQTools.getImageExistss(2, true, R.drawable.btr_serveysbl, R.drawable.btr_serveysbl_1) == 0)
                        {
                            clearrecents();
                            delay(2000);
                            continue;
                        }
                        delay(8000);

                        //region --- Vòng lặp checkSer ---
                        while (true)
                        {
                            int checkServey = HSQTools.getImageExistss(20, true, R.drawable.btr_accept, R.drawable.btr_serveysbl_click, R.drawable.btr_serveysbl_click_1, R.drawable.btr_accept_all);
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
                            else if (checkServey == 1 || checkServey == 4)
                            {
                                delay(5000);
                                continue;
                            }

                            delay(3000);
                            updateNotificationContent("check servey");

                            if (HSQTools.getImageExistss(2, false, R.drawable.btr_minutes) == 0)
                            {
                                if(timeCheckServey > 5) {
                                    globalHome();
                                    delay(2000);
                                    continue beginApp;
                                }
                                updateNotificationContent("không có servey, check lại sau 1 phút");
                                delay(60000);
                                HSQTools.getImageExistss(2, true, R.drawable.btr_refreshservey);
                                delay(5000);
                                timeCheckServey++;
                                continue;
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
                                    AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
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
                                    swipe(xCenter, yBot, xCenter, yTop, 1500);
                                    delay(2000);
                                    slsw++;

                                    AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
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
                                            swipe(xCenter, yTop, xCenter, yBot, 1500);
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
                                    int maxInRangeValue = -1;
                                    int minOutRangeValue = Integer.MAX_VALUE;

                                    for (int j = 0; j < AllPointsOK.size(); j++)
                                    {
                                        TextBlock block = AllPointsOK.get(j);
                                        if (block.text.toLowerCase().contains("points") || block.text.toLowerCase().contains("pts"))
                                        {
                                            try
                                            {
                                                int currentPoints = Integer.parseInt(block.text.replaceAll("[^\\d]", ""));
                                                TextBlock currentMinuteBlock = null;
                                                TextBlock currentStarBlock = null;

                                                for (int offset = 1; offset <= 2; offset++)
                                                {
                                                    if (j + offset < AllPointsOK.size())
                                                    {
                                                        String nextText = AllPointsOK.get(j + offset).text.toLowerCase();
                                                        if (nextText.contains("points") || nextText.contains("pts"))
                                                            break;
                                                        if (nextText.contains("min"))
                                                            currentMinuteBlock = AllPointsOK.get(j + offset);
                                                        else if (nextText.contains("/5"))
                                                            currentStarBlock = AllPointsOK.get(j + offset);
                                                    }
                                                }

                                                if (currentPoints >= 0 && currentPoints <= 150)
                                                {
                                                    if (currentPoints > maxInRangeValue)
                                                    {
                                                        maxInRangeValue = currentPoints;
                                                        minPointBlock = block;
                                                        minMinuteBlock = currentMinuteBlock;
                                                        minStarBlock = currentStarBlock;
                                                    }
                                                }
                                                else if (currentPoints < minOutRangeValue)
                                                {
                                                    minOutRangeValue = currentPoints;
                                                    if (minPointBlock == null)
                                                    {
                                                        minPointBlock = block;
                                                        minMinuteBlock = currentMinuteBlock;
                                                        minStarBlock = currentStarBlock;
                                                    }
                                                }
                                            }
                                            catch (Exception ignored)
                                            {
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
                                            checkPoints = HSQTools.readAllTextOnScreenByASBL(asblService.getRootInActiveWindow());
                                            if (checkPoints.isEmpty())
                                                checkPoints = convertXmlToTextBlocks(HSQTools.getFlexibleXML());

                                            TextBlock targetToClick = checkPoints.stream()
                                                    .filter(x -> x.text.replaceAll("[^\\d]", "").equals(targetPointNum))
                                                    .filter(x -> x.y > 410).findFirst().orElse(null);

                                            if (targetToClick != null)
                                            {
                                                click(targetToClick.x, targetToClick.y, false);
                                                writeSerLogs("Click thành công!"); // LOG THÀNH CÔNG
                                                answerOK = true;
                                                break;
                                            }
                                            swipe(xCenter, yBot, xCenter, yTop, 1500);
                                            delay(2000);
                                        }
                                        if (!answerOK)
                                        {
                                            writeSerLogs("Không tìm thấy tọa độ thẻ trên màn hình để click (Force click tọa độ cố định).");
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
                                HSQTools.getImageExistss(2, true, R.drawable.btr_english);
                                HSQTools.delay(3000);
                                click(640, 2819, false); // continue
                            }// ngon ngu
                            else if (checkSetup == 2 && LastInterFace != 2)
                            {
                                updateNotificationContent("giới tính");
                                if (InfoProfile.getGender() == null || InfoProfile.getGender() == RegistrationInfo.Gender.NONE)
                                {
                                    textAnswer = getAnswerFromGemByApi(1, true, true, "");
                                    updateNotificationContent("giới tính: " + textAnswer);
                                    if (normalizeText(textAnswer).contains("nam"))
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

                                inputText(inputAn, null, false);
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
                                    swipe(xCenter, yBot, xCenter, yTop, 1500);
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
                                    swipe(xCenter, yBot, xCenter, yTop, 1500);
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
                                click(704, 1218, false); // cau tl
                                HSQTools.delay(2000);
                                clearAllText();
                                HSQTools.delay(1000);
                                inputText(textAnswer, null, false);
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
                        buserLoop:
                        while (true)
                        {
                            scanFull = 0;
                            textAnswer = "";
                            dropDownOpen = false;
                            matrixColumnCache.clear();

                            int checkGDKS = HSQTools.getImageExistss(
                                    2, false,
                                    R.drawable.btr_serveyngao, R.drawable.btr_bamtieptuc, R.drawable.btr_captcha, R.drawable.btr_accept, R.drawable.btr_profile_match,
                                    R.drawable.btr_minutes, R.drawable.btr_refreshservey_1, R.drawable.btr_refreshservey, R.drawable.btr_ngaysinh, R.drawable.btr_gioitinh_tuoi_con,
                                    R.drawable.btr_toisinhra, R.drawable.btr_tinh_sv, R.drawable.btr_serveysbl, R.drawable.btr_serveysbl_1, R.drawable.btr_complete_profile,
                                    R.drawable.btr_accept_all
                            );
                            if (checkGDKS == 1)
                            {
                                clearrecents();
                                HSQTools.delay(2000);
                                continue beginApp; // Thay cho goto begin;
                            }
                            else if (checkGDKS == 2 && LastInterFace != 2)
                            {
                                click(720, 1305, false);
                            }
                            else if (checkGDKS == 3 && LastInterFace != 3)
                            {
                                HSQTools.sendTelegramAlert(deviceID, "captcha", idTelegram);
                                List<TextBlock> checkUserAct1 = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                while (true)
                                {
                                    updateNotificationContent("Lỗi: captcha");
                                    List<TextBlock> checkUserAct2 = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                    if (!HSQTools.areAlmostSame(checkUserAct1, checkUserAct2, 20))
                                    {
                                        break;
                                    }
                                    HSQTools.delay(180000);
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
                            else if (checkGDKS == 6 || checkGDKS == 7 || checkGDKS == 8 || checkGDKS == 13 || checkGDKS == 14)
                            {
                                if (HSQTools.getImageExistss(2, false, R.drawable.btr_servey_passed) != 0)
                                {
                                    writeSerLogs("servey passed");
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
                                swipe(1028, 2325, 1028, 1470, 1500);
                                HSQTools.delay(2000);

                                boolean YearOK = false;
                                while (true)
                                {
                                    List<TextBlock> checkYOB = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
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
                                                swipe(1028, YOBB.y, 1028, 1917, 1500);
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
                                    List<TextBlock> checkMOB = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                    for (TextBlock MOBB : checkMOB)
                                    {
                                        String checkY = normalizeText(MOBB.text);
                                        if (checkY.equals(MSub))
                                        {
                                            if (MOBB.y < 1850 || MOBB.y > 1980)
                                            {
                                                swipe(445, MOBB.y, 445, 1917, 1500);
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
                                if (!previousText.contains("step"))
                                {
                                    HSQTools.zoomOut();
                                    delay(2000);
                                }
                                if (checkGDKS == 9)
                                {
                                    tempTextAnswer = textAnswer = "begin|GDKS = 15|step1 dropdown {year}; step2 clicktotext {" + InfoProfile.getYearOfBirth() + "}; step3 dropdown {month};" +
                                            "step4 clicktotext {" + InfoProfile.getMonthOfBirth() + "}; step 5 dropdown {day}; step6 clicktotext {" + InfoProfile.getDayOfBirth() + "}; step7 clickbutton {continue}|end";
                                    currentState = 3;
                                }
                                else
                                {
                                    currentState = STATE_START;
                                }

                                stateMachine:
                                while (true)
                                {
                                    switch (currentState)
                                    {
                                        //region ---STATE_START (0)---
                                        case STATE_START:
                                            if (checkGDKS == 12)
                                            {
                                                tempTextAnswer = textAnswer = "begin|" + InfoProfile.getProvince() + "|step1 clicktotext {" + InfoProfile.getProvince() + "}; step2 clickbutton {tieptheo};|end";
                                                xs = xCenter;
                                                ysTop = yTop;
                                                ysBot = yBot;
                                                swipeDuration = 1500;
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
                                                    screenBegin = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
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
                                                List<HSQTools.TextBlock> checkUserAct1 = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                while (true)
                                                {
                                                    updateNotificationContent("Lỗi: captcha");
                                                    List<HSQTools.TextBlock> checkUserAct2 = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                    if (!HSQTools.areAlmostSame(checkUserAct1, checkUserAct2, 20))
                                                    {
                                                        break;
                                                    }
                                                    delay(180000);
                                                }
                                            }
                                            checkloi = 0;

                                            // region check xem có bị che màn không (Tương đương nhãn checkSame:)
                                            while (true)
                                            {
                                                daClick = false;
                                                screenDif = false;
                                                List<TextBlock> checkScreens = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());

                                                if (!HSQTools.areAlmostSame(screenBegin, checkScreens, 20))
                                                {
                                                    String resultNorms = normalizeText("tôi cần thêm thời gian");

                                                    for (TextBlock answer : checkScreens)
                                                    {
                                                        String answerChoose = normalizeText(answer.text);
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
                                                            String answerChoose = normalizeText(answer.text);
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
                                                            String answerChoose = normalizeText(answer.text);
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

                                            if (screenDif && previousText.contains("step"))
                                            {
                                                clickChoose++;
                                                if (textAnswer.contains("|swipemore|"))
                                                {
                                                    tempTextAnswer = previousText;
                                                }
                                                else
                                                {
                                                    tempTextAnswer = textAnswer;
                                                }
                                                currentState = STATE_ROLLBACK1; // goto rollBack1;
                                                continue;
                                            }

                                            if (tempSwipeCount > 0 && !textAnswer.contains("swipemore"))
                                            {
                                                for (int i = 0; i < tempSwipeCount; i++)
                                                {
                                                    swipe(xs, ysTop, xs, ysBot, swipeDuration);
                                                    delay(2000);
                                                }
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

                                                // =======================================================
                                                // 🛡️ LỚP PHÒNG NGỰ 1: "CHỐT CHẶN MÙ SƯƠNG" (LOCAL DATA HASHING)
                                                // Chạy trước khi đốt tiền gọi API Gemini
                                                // =======================================================
                                                try {
                                                    // 1. Cào toàn bộ Text trên màn hình bằng ASBL + OCR (Nhanh, tốn 0đ)
                                                    // Sử dụng luôn hàm getCheckAnswerSmart() sếp đã có
                                                    List<HSQTools.TextBlock> currentTexts = getCheckAnswerSmart();

                                                    // 2. Tạo "Dấu vân tay" (Hash) cho màn hình hiện tại
                                                    StringBuilder sb = new StringBuilder();
                                                    for(HSQTools.TextBlock tb : currentTexts) {
                                                        sb.append(tb.text);
                                                    }
                                                    String currentScreenHash = sb.toString();

                                                    // 3. Kiểm tra chốt chặn
                                                    if (!currentScreenHash.isEmpty() && currentScreenHash.equals(lastScreenHash)) {
                                                        stuckCounter++;
                                                        updateNotificationContent("⚠️ Báo động: Phát hiện màn hình bị kẹt lần " + stuckCounter);

                                                        if (stuckCounter >= MAX_STUCK_LIMIT) {
                                                            String errorMsg = "🛑 TỬ HÌNH KHẢO SÁT: Kẹt cứng ở một màn hình (" + stuckCounter + " lần). Đã ngắt mạch để chống đốt Quota API!";
                                                            updateNotificationContent(errorMsg);
                                                            String currentXml = HSQTools.getFlexibleXML();
                                                            List<HSQTools.TextBlock> currentVisible = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                            HSQFileHelper.deleteFile(imagePath);
                                                            HSQFileHelper.createFolder(imagePath);
                                                            delay(1000);
                                                            HSQTools.captureAndSaveScreen(imagePath + "/screenCap.png");
                                                            delay(2000);
                                                            HSQTools.ScanImage(imagePath);

                                                            // GỬI BÁO CÁO VIP LÊN TELEGRAM
                                                            HSQTools.sendTelegramAlertVIP(deviceID, errorMsg, idTelegram, imagePath, currentXml);

                                                            while (true)
                                                            {
                                                                List<HSQTools.TextBlock> act = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                                if (!HSQTools.areAlmostSame(currentVisible, act, 20)) break;
                                                                delay(180000);
                                                            }

                                                            // RESET biến để chuẩn bị cho Script/Account tiếp theo
                                                            lastScreenHash = "";
                                                            stuckCounter = 0;
                                                            currentState = STATE_ANSWER_OK; // goto answerOK;
                                                            continue;

                                                        }
                                                        else {
                                                            // Mới kẹt 1 lần (Có thể do click hụt, lag mạng) -> Cố gắng tự cứu bằng cách tác động vật lý
                                                            updateNotificationContent("Màn hình không chuyển, thử vuốt mồi phá kẹt...");
                                                            swipe(xs, ysBot, xs, ysBot - 300, 1000); // Vuốt nhích lên 1 chút ép render
                                                            delay(2000);
                                                            currentState = STATE_ANSWER_OK; // goto answerOK;
                                                            continue;
                                                        }
                                                    } else {
                                                        // 4. Màn hình đã chuyển sang cảnh mới an toàn!
                                                        stuckCounter = 0; // Reset bộ đếm lỗi
                                                        lastScreenHash = currentScreenHash; // Cập nhật lại Hash mới
                                                    }
                                                } catch (Exception e) {
                                                    updateNotificationContent("Lỗi khi tạo Hash màn hình: " + e.getMessage());
                                                }


                                                if (screenSwipe == 0)
                                                {
                                                    String currentXmlForSwipe = getFlexibleXML();
                                                    android.graphics.Rect dropBounds = HSQTools.findActiveDropdownBounds(currentXmlForSwipe);

                                                    if (dropBounds != null && dropBounds.height() > 400 && !currentXmlForSwipe.contains("RadioButton"))
                                                    {
                                                        xs = dropBounds.left + (dropBounds.width() / 2);
                                                        ysBot = dropBounds.bottom - 50;
                                                        ysTop = dropBounds.top + 50;
                                                        if (ysBot > yBot && (ysBot - ysTop > 300))
                                                        {
                                                            ysBot = yBot;
                                                        }
                                                        if (ysTop < heightOfScreen * 20 / 100)
                                                        {
                                                            ysTop = heightOfScreen * 20 / 100;
                                                        }
                                                        swipeDuration = Math.max(350, Math.min(1500, Math.abs(ysBot - ysTop)));
                                                    }
                                                    else
                                                    {
                                                        xs = xCenter;
                                                        ysTop = yTop;
                                                        ysBot = yBot;
                                                        swipeDuration = 1500;
                                                    }
                                                }

                                                delay(1000);
                                                HSQFileHelper.deleteFile(imagePath);
                                                HSQFileHelper.createFolder(imagePath);
                                                delay(1000);
                                                HSQTools.captureAndSaveScreen(imagePath + "/screenCapa1.png");

                                                while (true)
                                                {
                                                    List<HSQTools.TextBlock> beforeSwipe = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                    swipe(xs, ysBot, xs, ysTop, swipeDuration);
                                                    delay(3000);

                                                    screenBegin = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());

                                                    if (HSQTools.areAlmostSame(beforeSwipe, screenBegin, 20) || screenSwipe > 8)
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
                                                }

                                                tempSwipeCount = tempSwipeCount + screenSwipe;
                                                //region --local AI--
                                                boolean handledByLocalBrain = false;
//                                                String currentXmlForBrain = getFlexibleXML();
//                                                List<HSQTools.TextBlock> visibleTextsForBrain = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
//
//                                                String realHeader = findUnansweredHeaderForLocal(visibleTextsForBrain, currentXmlForBrain);
//
//                                                if (!realHeader.isEmpty())
//                                                {
//                                                    if (realHeader.equals("ALL_ANSWERED_CLICK_NEXT"))
//                                                    {
//                                                        // TẤT CẢ CÂU HỎI TRÊN MÀN HÌNH ĐÃ XONG -> RA LỆNH BẤM NEXT
//                                                        updateNotificationContent("✅ Local AI: Tất cả câu hỏi đã hoàn thành. Chuyển trang!");
//                                                        textAnswer = "begin|swipemore|1|step1 clickbutton {continue}|end";
//                                                        handledByLocalBrain = true;
//                                                    }
//                                                    else
//                                                    {
//                                                        // TÓM ĐƯỢC CÂU HỎI CHƯA TRẢ LỜI -> NÉM CHO BỘ NÃO XỬ LÝ
//                                                        String localCommand = LocalBrain.solveFormWithData(realHeader, allTextScanned, profileProvider);
//
//                                                        if (localCommand != null && !localCommand.contains("Back Button"))
//                                                        {
//                                                            updateNotificationContent("✅ True AI Offline xử gọn: " + realHeader);
//                                                            textAnswer = localCommand;
//                                                            handledByLocalBrain = true;
//                                                        }
//                                                    }
//                                                }
                                                //endregion
                                                // NẾU TẤT CẢ CÁC TRƯỜNG NHÂN KHẨU HỌC ĐỀU BÓ TAY, HOẶC LÀ CÂU HỎI THƯƠNG HIỆU/HÀNH VI -> GỌI API LỚN!
                                                if (!handledByLocalBrain)
                                                {
                                                    updateNotificationContent("Local mù/Câu hỏi phụ, bắn " + (screenSwipe + 1) + " ảnh cho API...");
                                                    textAnswer = getAnswerFromGemByApi(screenSwipe + 1, false, false, "");
                                                }

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
                                                    for (int p = 0; p < splitStep.length; p++)
                                                    {
                                                        step = "";
                                                        if (clickChoose > 0 && !dropDownOpen)
                                                        {
                                                            step = previousText;
                                                        }
                                                        else
                                                        {
                                                            step = splitStep[p];
                                                        }
                                                        updateNotificationContent("thực hiện: " + step);
                                                        if (step.contains("clicktotext"))
                                                        {
                                                            Matcher match = java.util.regex.Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                            if (match.find())
                                                            {
                                                                while (true)
                                                                {
                                                                    String result = match.group(1);
                                                                    String resultNorm = HSQTools.getOnlyTextLinq(normalizeText(result));
                                                                    List<TextBlock> temp = new ArrayList<>();
                                                                    int vuotLenLai = 0, checkLaiScreen = 0;

                                                                    timTextLoop:
                                                                    while (true)
                                                                    {
                                                                        // Lấy toàn bộ TextBlock trên màn hình
                                                                        List<TextBlock> checkAnswer = getCheckAnswerSmart().stream()
                                                                                .filter(x -> x.y > 180 && x.y < 2800).collect(Collectors.toList());

                                                                        while (true)
                                                                        {
                                                                            // Logic chống kẹt màn hình
                                                                            if (HSQTools.areAlmostSame(temp, checkAnswer, 20))
                                                                            {
                                                                                if (vuotLenLai == 0)
                                                                                {
                                                                                    vuotLenLai++;
                                                                                    swipe(xs, ysTop, xs, ysBot, swipeDuration);
                                                                                    delay(2000);
                                                                                }
                                                                                else
                                                                                {
                                                                                    handleActionFailure(
                                                                                            "clicktotext", step, checkAnswer,
                                                                                            "Lỗi clicktotext: Vuốt nát màn hình đéo thấy chữ [" + step + "]",
                                                                                            splitTextAnswer[1]
                                                                                    );
                                                                                    tempTextAnswer = textAnswer;
                                                                                    continue stateMachine;
                                                                                }
                                                                            }

                                                                            // --------------------------------------------------------
                                                                            // 🎯 TẦNG 1: KHỚP TUYỆT ĐỐI (EQUALS) - CHỐT LUÔN!
                                                                            // --------------------------------------------------------
                                                                            List<TextBlock> candidates1 = new ArrayList<>();
                                                                            for (TextBlock answer : checkAnswer)
                                                                            {
                                                                                String answerChoose = HSQTools.getOnlyTextLinq(normalizeText(answer.text));
                                                                                if (answerChoose.equals(resultNorm) ||
                                                                                        (answerChoose.startsWith("o") && answerChoose.substring(1).equals(resultNorm)) ||
                                                                                        ((answerChoose.startsWith("l") || answerChoose.startsWith("1")) && resultNorm.startsWith("i") && ("i" + answerChoose.substring(1)).equals(resultNorm)))
                                                                                {
                                                                                    candidates1.add(answer);
                                                                                }
                                                                            }

                                                                            if (!candidates1.isEmpty())
                                                                            {
                                                                                // 🌟 THUẬT TOÁN SẮP XẾP MỚI: CHỐNG CHỌT NHẦM TEXT RÁC TRÊN ĐỈNH
                                                                                // Nếu có nhiều đáp án khớp tuyệt đối (VD: 3 chữ "Co" trên màn hình)
                                                                                // BẮT BUỘC Ưu tiên thằng NẰM DƯỚI CÙNG (Y lớn nhất) vì đáp án thường nằm dưới mô tả.
                                                                                TextBlock target = (candidates1.size() == 1) ? candidates1.get(0) :
                                                                                        candidates1.stream().max(Comparator.comparingInt(c -> c.y)).orElse(null);

                                                                                if (target != null)
                                                                                {
                                                                                    int finalClickX = target.x;
                                                                                    int finalClickY = target.y;

                                                                                    // 🌟 BỌC THÉP X TẦNG CAO: DÙNG XML ĐỂ ÉP VỀ LỖ RADIO
                                                                                    String currentXmlForX = HSQTools.getFlexibleXML();
                                                                                    try
                                                                                    {
                                                                                        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                        org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXmlForX.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                        org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                        int snappedX = -1;
                                                                                        int minDiffX = widthOfScreen; // Tìm Node XML nào gần với X của TextBlock nhất

                                                                                        for (int i = 0; i < nodes.getLength(); i++)
                                                                                        {
                                                                                            org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                            android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                                                                                            // Bắt buộc Node XML phải cùng dòng Y (sai số 40px)
                                                                                            // 🌟 THÊM BỌC THÉP TỌA ĐỘ 0: Loại bỏ ngay những node có centerX <= 0
                                                                                            if (r != null && Math.abs(r.centerY() - finalClickY) <= 40 && r.centerX() > 0)
                                                                                            {
                                                                                                String xmlText = HSQTools.getOnlyTextLinq(normalizeText(node.getAttribute("text") + " " + node.getAttribute("content-desc")));

                                                                                                // Ưu tiên 1: Có lỗ Radio thật (Class là RadioButton hoặc kích thước nhỏ dạng cục)
                                                                                                boolean isRadioButton = node.getAttribute("class").contains("RadioButton");
                                                                                                boolean isSmallBox = r.width() > 20 && r.width() < 150 && r.height() > 20;

                                                                                                if (isRadioButton || isSmallBox) {
                                                                                                    snappedX = r.centerX();
                                                                                                    break; // Thấy lỗ thật (X>0) hít mẹ vào luôn đéo lằng nhằng
                                                                                                }

                                                                                                // Ưu tiên 2: XML khớp chữ và bóp X lùi về mép trái (Chống hộp Full Width)
                                                                                                if (xmlText.equals(resultNorm) || xmlText.contains(resultNorm)) {
                                                                                                    if (r.width() < 600) {
                                                                                                        int diff = Math.abs(r.centerX() - finalClickX);
                                                                                                        if (diff < minDiffX) {
                                                                                                            minDiffX = diff;
                                                                                                            snappedX = r.centerX();
                                                                                                        }
                                                                                                    } else if (r.width() >= 600) {
                                                                                                        // Nếu là hộp to (Ví dụ: [0, 500][1440, 600]), tịnh tiến X về lề trái
                                                                                                        // Đảm bảo kết quả không bị lùi về 0 hoặc âm
                                                                                                        int newX = r.left + 50;
                                                                                                        if (newX > 0) {
                                                                                                            snappedX = newX;
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        if (snappedX > 0) finalClickX = snappedX; // Chỉ gán khi kết quả hợp lệ
                                                                                    }
                                                                                    catch (Exception ignored) {}

                                                                                    updateNotificationContent("Click Text: Chọt [" + resultNorm + "] tại X=" + finalClickX + ", Y=" + finalClickY);
                                                                                    click(finalClickX, finalClickY, false);
                                                                                    previousText = resultNorm;
                                                                                    break timTextLoop;
                                                                                }
                                                                            }

                                                                            // --------------------------------------------------------
                                                                            // 🎯 TẦNG 2: KHỚP CHỨA (CONTAINS)
                                                                            // --------------------------------------------------------
                                                                            long digitCount = resultNorm.chars().filter(Character::isDigit).count();

                                                                            if (digitCount < 3)
                                                                            {
                                                                                List<TextBlock> candidates2 = new ArrayList<>();
                                                                                for (TextBlock answer : checkAnswer)
                                                                                {
                                                                                    String answerChoose = HSQTools.getOnlyTextLinq(normalizeText(answer.text));
                                                                                    boolean isContains = answerChoose.contains(resultNorm) || (answerChoose.length() >= 5 && resultNorm.contains(answerChoose));

                                                                                    if (isContains)
                                                                                    {
                                                                                        int lenDiff = Math.abs(answerChoose.length() - resultNorm.length());
                                                                                        if (lenDiff <= 10 || (float) answerChoose.length() / resultNorm.length() <= 2.5f)
                                                                                        {
                                                                                            candidates2.add(answer);
                                                                                        }
                                                                                    }
                                                                                }

                                                                                if (!candidates2.isEmpty())
                                                                                {
                                                                                    final String finalResultNorm = resultNorm;
                                                                                    // Tương tự Tầng 1: Ưu tiên Y lớn nhất (Nằm thấp nhất màn hình) thay vì Y nhỏ nhất
                                                                                    TextBlock target = (candidates2.size() == 1) ? candidates2.get(0) :
                                                                                            candidates2.stream().max((c1, c2) ->
                                                                                            {
                                                                                                int dist1 = HSQTools.levenshtein(HSQTools.getOnlyTextLinq(normalizeText(c1.text)), finalResultNorm);
                                                                                                int dist2 = HSQTools.levenshtein(HSQTools.getOnlyTextLinq(normalizeText(c2.text)), finalResultNorm);
                                                                                                // Nếu lệch Levenshtein thì ưu tiên độ chuẩn xác (dist nhỏ).
                                                                                                // Nếu bằng nhau độ chuẩn xác, ưu tiên Y LỚN (Max Y).
                                                                                                return (dist1 == dist2) ? Integer.compare(c1.y, c2.y) : Integer.compare(dist2, dist1);
                                                                                            }).orElse(null);

                                                                                    if (target != null)
                                                                                    {
                                                                                        int finalClickX = target.x;
                                                                                        int finalClickY = target.y;

                                                                                        String currentXmlForX = HSQTools.getFlexibleXML();
                                                                                        try
                                                                                        {
                                                                                            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXmlForX.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                            int snappedX = -1;
                                                                                            for (int i = 0; i < nodes.getLength(); i++)
                                                                                            {
                                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                                if (r != null && Math.abs(r.centerY() - finalClickY) <= 40)
                                                                                                {
                                                                                                    boolean isRadioButton = node.getAttribute("class").contains("RadioButton");
                                                                                                    boolean isSmallBox = r.width() > 20 && r.width() < 150 && r.height() > 20;

                                                                                                    if (isRadioButton || isSmallBox) {
                                                                                                        snappedX = r.centerX();
                                                                                                        break;
                                                                                                    }

                                                                                                    String xmlText = HSQTools.getOnlyTextLinq(normalizeText(node.getAttribute("text") + " " + node.getAttribute("content-desc")));
                                                                                                    if (xmlText.equals(resultNorm) || xmlText.contains(resultNorm)) {
                                                                                                        if (r.width() < 600 && r.centerX() > 0) {
                                                                                                            snappedX = r.centerX();
                                                                                                        } else if (r.width() >= 600) {
                                                                                                            snappedX = r.left + 50;
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                            if (snappedX != -1) finalClickX = snappedX;
                                                                                        }
                                                                                        catch (Exception ignored) {}

                                                                                        updateNotificationContent("Click Text (Contains): Chọt [" + resultNorm + "] tại Y=" + finalClickY);
                                                                                        click(finalClickX, finalClickY, false);
                                                                                        previousText = resultNorm;
                                                                                        break timTextLoop;
                                                                                    }
                                                                                }
                                                                            }

                                                                            // --------------------------------------------------------
                                                                            // 🎯 TẦNG 3: KHỚP SỰ KIỆN SAI SỐ (LEVENSHTEIN)
                                                                            // --------------------------------------------------------
                                                                            if (digitCount < 3)
                                                                            {
                                                                                List<TextBlock> candidates3 = new ArrayList<>();
                                                                                for (TextBlock answer : checkAnswer)
                                                                                {
                                                                                    String answerChoose = HSQTools.getOnlyTextLinq(normalizeText(answer.text));
                                                                                    int distance = HSQTools.levenshtein(answerChoose, resultNorm);
                                                                                    if (distance <= (int) (resultNorm.length() * 0.25))
                                                                                    {
                                                                                        candidates3.add(answer);
                                                                                    }
                                                                                }

                                                                                if (!candidates3.isEmpty())
                                                                                {
                                                                                    final String finalResultNorm = resultNorm;
                                                                                    TextBlock target = (candidates3.size() == 1) ? candidates3.get(0) :
                                                                                            candidates3.stream().max((c1, c2) ->
                                                                                            {
                                                                                                int dist1 = HSQTools.levenshtein(HSQTools.getOnlyTextLinq(normalizeText(c1.text)), finalResultNorm);
                                                                                                int dist2 = HSQTools.levenshtein(HSQTools.getOnlyTextLinq(normalizeText(c2.text)), finalResultNorm);
                                                                                                return (dist1 == dist2) ? Integer.compare(c1.y, c2.y) : Integer.compare(dist2, dist1);
                                                                                            }).orElse(null);

                                                                                    if (target != null)
                                                                                    {
                                                                                        int finalClickX = target.x;
                                                                                        int finalClickY = target.y;

                                                                                        String currentXmlForX = HSQTools.getFlexibleXML();
                                                                                        try
                                                                                        {
                                                                                            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXmlForX.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                            int snappedX = -1;
                                                                                            for (int i = 0; i < nodes.getLength(); i++)
                                                                                            {
                                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                                if (r != null && Math.abs(r.centerY() - finalClickY) <= 40)
                                                                                                {
                                                                                                    boolean isRadioButton = node.getAttribute("class").contains("RadioButton");
                                                                                                    boolean isSmallBox = r.width() > 20 && r.width() < 150 && r.height() > 20;

                                                                                                    if (isRadioButton || isSmallBox) {
                                                                                                        snappedX = r.centerX();
                                                                                                        break;
                                                                                                    }

                                                                                                    String xmlText = HSQTools.getOnlyTextLinq(normalizeText(node.getAttribute("text") + " " + node.getAttribute("content-desc")));
                                                                                                    if (xmlText.equals(resultNorm) || xmlText.contains(resultNorm)) {
                                                                                                        if (r.width() < 600 && r.centerX() > 0) {
                                                                                                            snappedX = r.centerX();
                                                                                                        } else if (r.width() >= 600) {
                                                                                                            snappedX = r.left + 50;
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                            if (snappedX != -1) finalClickX = snappedX;
                                                                                        }
                                                                                        catch (Exception ignored) {}

                                                                                        updateNotificationContent("Click Text (Sai số): Chọt [" + resultNorm + "] tại Y=" + finalClickY);
                                                                                        click(finalClickX, finalClickY, false);
                                                                                        previousText = resultNorm;
                                                                                        break timTextLoop;
                                                                                    }
                                                                                }
                                                                            }

                                                                            // --------------------------------------------------------
                                                                            // 🎯 TẦNG DỰ PHÒNG: LOGIC ĐẶC BIỆT NỮ/FEMALE
                                                                            // --------------------------------------------------------
                                                                            if (resultNorm.equals("nu") || resultNorm.equals("female"))
                                                                            {
                                                                                TextBlock qNode = checkAnswer.stream().filter(x -> normalizeText(x.text).contains("gioitinh")).findFirst().orElse(null);
                                                                                TextBlock mNode = checkAnswer.stream().filter(x -> normalizeText(x.text).contains("nam")).findFirst().orElse(null);
                                                                                if (qNode != null && mNode != null)
                                                                                {
                                                                                    click(mNode.x, qNode.y + ((mNode.y - qNode.y) / 2), false);
                                                                                    previousText = resultNorm;
                                                                                    break timTextLoop;
                                                                                }
                                                                            }

                                                                            // --------------------------------------------------------
                                                                            // Fallback Vuốt
                                                                            // --------------------------------------------------------
                                                                            if (checkLaiScreen == 0)
                                                                            {
                                                                                checkAnswer = getOcrTextBlocks().stream().filter(x -> x.y > 180 && x.y < 2750).collect(Collectors.toList());
                                                                                checkLaiScreen++;
                                                                                continue;
                                                                            }
                                                                            checkLaiScreen = 0;
                                                                            temp = checkAnswer;
                                                                            if (vuotLenLai == 0)
                                                                            {
                                                                                swipe(xs, ysBot, xs, ysTop, swipeDuration);
                                                                            }
                                                                            else
                                                                            {
                                                                                swipe(xs, ysTop, xs, ysBot, swipeDuration);
                                                                            }
                                                                            delay(2000);
                                                                            break;
                                                                        }
                                                                    }
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        else if (step.contains("clickbutton"))
                                                        {
                                                            if (checkGDKS == 9 || checkGDKS == 15)
                                                            {
                                                                click(720, 2829, false);//continue
                                                                delay(10000);
                                                                continue lamProfileLoop;
                                                            }

                                                            // 🌟 ĐỘC CHIÊU THEO Ý SẾP: Trích xuất chính xác cụm chữ trong ngoặc nhọn {} do Gemini truyền xuống
                                                            Matcher matchBtn = java.util.regex.Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                            String rawTarget = matchBtn.find() ? matchBtn.group(1).trim() : "";

                                                            // 🔥 BỌC THÉP 1 (BẢO TỒN KÝ TỰ): Nếu lệnh là mũi tên (->, >, →) thì cấm normalize để không bị mất đuôi!
                                                            boolean isArrow = rawTarget.contains(">") || rawTarget.contains("->") || rawTarget.contains("→");
                                                            final String targetNorm = isArrow ? rawTarget : HSQTools.getOnlyTextLinq(normalizeText(rawTarget));

                                                            checkButtonAgainLoop:
                                                            while (true)
                                                            {
                                                                List<HSQTools.TextBlock> tempCompare = new ArrayList<>();
                                                                int vuotLenLai = 0;

                                                                while (true)
                                                                {
                                                                    // TẦNG 1: ƯU TIÊN HÌNH ẢNH
                                                                    if (HSQTools.getImageExistss(
                                                                            2, true, R.drawable.btr_nextser_niq, R.drawable.btr_nextser_es, R.drawable.btr_nextser,
                                                                            R.drawable.btr_next_ifm, R.drawable.btr_next_niq1
                                                                    ) != 0)
                                                                    {
                                                                        break checkButtonAgainLoop;
                                                                    }

                                                                    // TẦNG 2: DÙNG MẮT THẦN THÔNG MINH (ASBL -> XML -> OCR)
                                                                    List<HSQTools.TextBlock> smartList = getCheckAnswerSmart();

                                                                    // 🌟 2.1: KIỂM TRA ĐẶC NHIỆM (ĐẠI ĐỘI NÚT TRỐNG)
                                                                    // Cứu cánh nếu Gemini bảo bấm ">" nhưng XML đéo có text
                                                                    if (isArrow || targetNorm.equals(">") || targetNorm.equals(">>") || targetNorm.equals("->"))
                                                                    {
                                                                        try
                                                                        {
                                                                            String xmlForArrow = HSQTools.getFlexibleXML();
                                                                            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlForArrow.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                            for (int i = 0; i < nodes.getLength(); i++)
                                                                            {
                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                String clazz = node.getAttribute("class");
                                                                                String text = node.getAttribute("text");
                                                                                String desc = node.getAttribute("content-desc");

                                                                                // Nút nằm ở nửa dưới, là dạng Button/Image, đéo có chữ
                                                                                if ((clazz.contains("Button") || clazz.contains("ImageView") || clazz.contains("Image"))
                                                                                        && text.trim().isEmpty() && desc.trim().isEmpty())
                                                                                {

                                                                                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                    if (r != null && r.centerY() > 1000)
                                                                                    { // Nằm nửa dưới màn hình
                                                                                        updateNotificationContent("Tóm được NÚT ẢNH TRỐNG (Nghi ngờ là dấu >) tại " + r.centerX() + "," + r.centerY());
                                                                                        click(r.centerX(), r.centerY(), false);
                                                                                        break checkButtonAgainLoop;
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                        catch (Exception ignored)
                                                                        {
                                                                        }
                                                                    }

                                                                    // 🌟 2.2: LƯỚI QUÉT TIÊU CHUẨN (CÓ CHỮ THÌ MỚI BẤM)
                                                                    HSQTools.TextBlock btnSmart = smartList.stream()
                                                                            .filter(x -> x.y > 180)
                                                                            .filter(x ->
                                                                            {
                                                                                // Lấy cả 2 bản: Bản gốc (để check mũi tên) và Bản gọt sạch (để check Regex)
                                                                                String rawText = x.text.trim();
                                                                                String cleanText = HSQTools.getOnlyTextLinq(normalizeText(x.text));

                                                                                if (rawText.isEmpty()) return false;

                                                                                // 🔥 ƯU TIÊN 1: Khớp chuẩn xác theo chỉ định của Gemini
                                                                                if (!targetNorm.isEmpty())
                                                                                {
                                                                                    if (isArrow) {
                                                                                        // Nếu tìm mũi tên, soi luôn bản gốc xem có bám trên chữ không (Ví dụ: "Trang tiếp theo >")
                                                                                        if (rawText.equals(targetNorm) || rawText.contains(targetNorm)) return true;
                                                                                    } else {
                                                                                        // Nếu là chữ thường, dùng bản gọt sạch
                                                                                        if (cleanText.equals(targetNorm)) return true;
                                                                                        if (targetNorm.length() >= 2 && cleanText.contains(targetNorm)) return true;
                                                                                        if (cleanText.length() >= 3 && targetNorm.contains(cleanText)) return true;
                                                                                    }
                                                                                }

                                                                                // 🌟 ƯU TIÊN 2: Bộ từ khóa dự phòng (LUÔN DÙNG BẢN GỌT SẠCH ĐỂ ĐỌ REGEX)
                                                                                // Bổ sung thêm "trangtieptheo" vào để tóm gọn Qualtrics
                                                                                return cleanText.matches("^(continue|next|submit|tieptuc|tieptheo|trangtieptheo|tieptheo>|done|gui|send|batdau|agree|accept|agreeandcontinue|>|>>|>>>|gotonextquestion|fwd|forward|tiep)$");
                                                                            })
                                                                            .max(Comparator.comparingInt((HSQTools.TextBlock x) -> x.y)) // Ưu tiên nút ở THẤP NHẤT
                                                                            .orElse(null);

                                                                    if (btnSmart != null)
                                                                    {
                                                                        updateNotificationContent("Tìm thấy nút chữ xịn: " + btnSmart.text + " tại (" + btnSmart.x + "," + btnSmart.y + ")");
                                                                        click(btnSmart.x, btnSmart.y, false);
                                                                        break checkButtonAgainLoop;
                                                                    }

                                                                    // TẦNG 3: TRUY QUÉT DỰ PHÒNG BẰNG XML (ĐỘ LẠI ĐỂ TÓM CỔ MỌI LOẠI NÚT NEXT/BUTTON)
                                                                    String currentXml = HSQTools.getFlexibleXML();
                                                                    try
                                                                    {
                                                                        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                        org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                        org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                        android.graphics.Rect bestXmlBtnRect = null;
                                                                        int maxCenterY = 0;

                                                                        for (int i = 0; i < nodes.getLength(); i++)
                                                                        {
                                                                            org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                            String text = node.getAttribute("text");
                                                                            String desc = node.getAttribute("content-desc");
                                                                            String resId = node.getAttribute("resource-id");

                                                                            // 🔥 BỌC THÉP TỬ HÌNH: Ép gom cả text lẫn desc lại, cấm bỏ sót như trước!
                                                                            String rawFullText = text + " " + desc;
                                                                            String cleanFullText = HSQTools.getOnlyTextLinq(normalizeText(rawFullText));

                                                                            boolean isMatch = false;

                                                                            // Điều kiện 1: Khớp chữ từ Gemini truyền xuống
                                                                            if (!targetNorm.isEmpty())
                                                                            {
                                                                                if (isArrow) {
                                                                                    if (rawFullText.contains(targetNorm)) isMatch = true;
                                                                                } else {
                                                                                    if (cleanFullText.equals(targetNorm) || (targetNorm.length() >= 2 && cleanFullText.contains(targetNorm))) {
                                                                                        isMatch = true;
                                                                                    }
                                                                                }
                                                                            }

                                                                            // Điều kiện 2: Bộ từ khóa dự phòng mạnh mẽ & CHỐT CHẶN RES-ID
                                                                            if (!isMatch)
                                                                            {
                                                                                // Thêm "trangtieptheo" vào Regex, và chỉ cần resId chứa chữ "next" hoặc "continue" là bắt gọn!
                                                                                if (cleanFullText.matches("^(continue|next|submit|tieptuc|tieptheo|trangtieptheo|tieptheo>|done|gui|send|batdau|agree|accept|agreeandcontinue|>|>>|>>>|gotonextquestion|fwd|forward|tiep)$")
                                                                                        || resId.toLowerCase().contains("next")
                                                                                        || resId.toLowerCase().contains("continue"))
                                                                                {
                                                                                    isMatch = true;
                                                                                }
                                                                            }

                                                                            if (isMatch)
                                                                            {
                                                                                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                if (r != null && r.height() > 20 && r.width() > 20 && r.centerY() > 180 && r.centerY() < 2950)
                                                                                {
                                                                                    if (r.centerY() > maxCenterY)
                                                                                    {
                                                                                        maxCenterY = r.centerY();
                                                                                        bestXmlBtnRect = r;
                                                                                    }
                                                                                }
                                                                            }
                                                                        }

                                                                        if (bestXmlBtnRect != null)
                                                                        {
                                                                            updateNotificationContent("Bắt sống nút Next từ XML tại " + bestXmlBtnRect.centerX() + "," + bestXmlBtnRect.centerY());
                                                                            click(bestXmlBtnRect.centerX(), bestXmlBtnRect.centerY(), false);
                                                                            break checkButtonAgainLoop;
                                                                        }
                                                                    }
                                                                    catch (Exception ignored)
                                                                    {
                                                                    }

                                                                    // TẦNG 4: XỬ LÝ VUỐT MÀN HÌNH TÌM NÚT KHI TẤT CẢ ĐỀU MÙ
                                                                    List<HSQTools.TextBlock> currentVisible = smartList.stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                                    if (HSQTools.areAlmostSame(tempCompare, currentVisible, 20))
                                                                    {
                                                                        if (vuotLenLai == 0)
                                                                        {
                                                                            vuotLenLai++;
                                                                            updateNotificationContent("Cuộn xuống kịch biên không thấy nút, quay xe cuộn lên!");
                                                                        }
                                                                        else
                                                                        {
                                                                            handleActionFailure(
                                                                                    "clickbutton", step, currentVisible,
                                                                                    "Mày bảo tao bấm nút [" + step + "] nhưng tao đã lật tung cả cái màn hình lên đéo thấy cái nút nào cả, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời",
                                                                                    splitTextAnswer[1]
                                                                            );
                                                                            tempTextAnswer = textAnswer;
                                                                            continue stateMachine;
                                                                        }
                                                                    }

                                                                    tempCompare = new ArrayList<>(currentVisible);

                                                                    if (vuotLenLai == 0)
                                                                    {
                                                                        swipe(xs, ysBot, xs, ysTop, swipeDuration);
                                                                    }
                                                                    else
                                                                    {
                                                                        swipe(xs, ysTop, xs, ysBot, swipeDuration);
                                                                    }
                                                                    delay(2000);
                                                                }
                                                            }
                                                        }
                                                        else if (step.contains("clearalltext"))
                                                        {
                                                            clearAllText();
                                                        }
                                                        else if (step.contains("swipe"))
                                                        {
                                                            Matcher match = java.util.regex.Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                            if (match.find())
                                                            {
                                                                String result = match.group(1);
                                                                String[] coordinates = result.split(",");
                                                                int x1 = Integer.parseInt(coordinates[0]);
                                                                int y1 = Integer.parseInt(coordinates[1]);
                                                                int x2 = Integer.parseInt(coordinates[2]);
                                                                int y2 = Integer.parseInt(coordinates[3]);
                                                                int timeSwipe = Integer.parseInt(coordinates[4]);
                                                                swipe(x1, y1, x2, y2, timeSwipe);
                                                            }
                                                        }
                                                        else if (step.contains("input"))
                                                        {
                                                            checkInputSmartLoop:
                                                            while (true)
                                                            {
                                                                Matcher match = java.util.regex.Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                                if (match.find() && match.group(1).contains("~"))
                                                                {
                                                                    String[] parts = match.group(1).split("~");
                                                                    String labelToFind = parts[0];
                                                                    String valueToInput = parts[1];

                                                                    // --- INTERCEPTOR: NẮN NÃO AI NẾU NÓ NHẬP NGU CẤP HÀNH CHÍNH ---
                                                                    String labelLower = normalizeText(labelToFind);
                                                                    String valueLower = normalizeText(valueToInput);
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

                                                                    updateNotificationContent("Đang săn tìm ô nhập: " + labelToFind);
                                                                    List<HSQTools.TextBlock> tempCompare = new ArrayList<>();
                                                                    int vuotLenLai = 0; // 0: Đang cuộn xuống tìm, 1: Đang cuộn ngược lên tìm

                                                                    while (true)
                                                                    {
                                                                        List<TextBlock> currentScreen = getCheckAnswerSmart();
                                                                        List<TextBlock> currentVisible = currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList());

                                                                        int inputX = -1, inputY = -1;
                                                                        // =========================================================
                                                                        // 🎯 1. TẦNG BỌC THÉP XML: BẮT SÁT NÁCH ĐỂ CHỐNG LOẠN FORM
                                                                        // =========================================================
                                                                        String xml = HSQTools.getFlexibleXML();
                                                                        try
                                                                        {
                                                                            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                            boolean foundLabel = false;
                                                                            int labelBottom = -1;
                                                                            String normTarget = HSQTools.getOnlyTextLinq(normalizeText(labelToFind));

                                                                            for (int i = 0; i < nodes.getLength(); i++)
                                                                            {
                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);

                                                                                if (!foundLabel)
                                                                                {
                                                                                    String text = node.getAttribute("text");
                                                                                    String desc = node.getAttribute("content-desc");
                                                                                    String clazz = node.getAttribute("class");
                                                                                    String combined = HSQTools.getOnlyTextLinq(normalizeText(text + " " + desc));

                                                                                    // Tìm đúng Label (VD: "Month", "Day")
                                                                                    if (!combined.isEmpty() && (combined.equals(normTarget) || combined.contains(normTarget)))
                                                                                    {
                                                                                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                        if (r != null)
                                                                                        {
                                                                                            foundLabel = true;
                                                                                            labelBottom = r.bottom;

                                                                                            // Trường hợp 1: Chính cái Label đó là ô nhập (Placeholder)
                                                                                            if (clazz.contains("EditText"))
                                                                                            {
                                                                                                inputX = r.centerX();
                                                                                                inputY = r.centerY();
                                                                                                updateNotificationContent("XML: Tóm được Placeholder tại Y=" + inputY);
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                                else
                                                                                {
                                                                                    // Trường hợp 2: Đã tìm thấy Label, đi tìm ô nhập nằm NGAY DƯỚI nó
                                                                                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                    // Ép điều kiện: Ô nhập phải nằm DƯỚI label (dung sai 50px)
                                                                                    // Và KHÔNG CÁCH QUÁ XA (300px) để chống nhảy cẩu thả sang ô Day/Year
                                                                                    if (r != null && r.top >= labelBottom - 50 && r.top <= labelBottom + 1200)
                                                                                    {
                                                                                        String clazz = node.getAttribute("class");
                                                                                        if (clazz.contains("EditText") || clazz.contains("AutoCompleteTextView"))
                                                                                        {
                                                                                            inputX = r.centerX();
                                                                                            inputY = r.centerY();
                                                                                            updateNotificationContent("XML: Tóm được Sibling EditText tại Y=" + inputY);
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                        catch (Exception ignored)
                                                                        {
                                                                        }

                                                                        // =========================================================
                                                                        // 🎯 2. FALLBACK: DÙNG ASBL NẾU XML MÙ (GIỮ LẠI LÀM CỨU CÁNH)
                                                                        // =========================================================
                                                                        if (inputX == -1 && inputY == -1)
                                                                        {
                                                                            android.graphics.Point labelPt = HSQTools.smartFindTextPoint(labelToFind);
                                                                            if (labelPt != null)
                                                                            {
                                                                                AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
                                                                                android.graphics.Rect inputRect = findInputNearYByASBL(root, labelPt.y);
                                                                                if (inputRect != null)
                                                                                {
                                                                                    inputX = inputRect.centerX();
                                                                                    inputY = inputRect.centerY();
                                                                                    updateNotificationContent("ASBL: Tóm được ô nhập tại Y=" + inputY);
                                                                                }
                                                                                if (root != null)
                                                                                    root.recycle();
                                                                            }
                                                                        }

                                                                        // 2. THỰC THI NHẬP TEXT NẾU ĐÃ TÌM THẤY Ô
                                                                        if (inputX != -1 && inputY != -1)
                                                                        {
                                                                            updateNotificationContent("Click ô nhập: " + inputX + "," + inputY);
                                                                            click(inputX, inputY, false);
                                                                            delay(3000);

                                                                            if (HSQTools.isKeyboardVisibleSmart())
                                                                            {
                                                                                clearAllText();
                                                                                delay(1000);
                                                                                inputText(valueToInput, null, false);
                                                                                delay(2500); // Chờ list load (nếu có)
                                                                                globalBack();
                                                                                delay(2000);
                                                                                break checkInputSmartLoop; // Hoàn thành ô nhập thường, thoát!
                                                                            }
                                                                            else
                                                                            {
                                                                                handleActionFailure(
                                                                                        "Input_Keyboard", step, currentVisible,
                                                                                        "Tao click vào ô nhập rồi nhưng bàn phím đéo lên!, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời",
                                                                                        splitTextAnswer[1]
                                                                                );
                                                                                tempTextAnswer = textAnswer;
                                                                                continue stateMachine;
                                                                            }
                                                                        }

                                                                        // --- 3. THUẬT TOÁN VUỐT QUÉT SẠCH FORM DÙNG AREALMOSTSAME ---
                                                                        if (HSQTools.areAlmostSame(tempCompare, currentVisible, 20))
                                                                        {
                                                                            if (vuotLenLai == 0)
                                                                            {
                                                                                // Đã cuộn xuống tới đáy xã hội mà không thấy -> Quay xe cuộn ngược lên lại
                                                                                vuotLenLai++;
                                                                                updateNotificationContent("Chạm đáy form! Quay xe cuộn ngược lên để tìm...");
                                                                            }
                                                                            else
                                                                            {
                                                                                // Đã xuống tận đáy, lên tận đỉnh, màn hình đứng im mà vẫn không thấy -> THỰC SỰ LỖI!
                                                                                handleActionFailure(
                                                                                        "Input_NotFound", step, currentVisible,
                                                                                        "Lỗi Input: Tao đã cuộn nát cái form này từ đỉnh xuống đáy rồi ngược lại mà đéo thấy ô nhập [" + labelToFind + "] đâu cả!, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời",
                                                                                        splitTextAnswer[1]
                                                                                );
                                                                                tempTextAnswer = textAnswer;
                                                                                continue stateMachine;
                                                                            }
                                                                        }

                                                                        // Lưu cấu trúc màn hình hiện tại để lượt sau so sánh kẹt
                                                                        tempCompare = new ArrayList<>(currentVisible);

                                                                        // Tiến hành vuốt dựa trên trạng thái "Quay xe" (vuotLenLai)
                                                                        if (vuotLenLai == 0)
                                                                        {
                                                                            swipe(xs, ysBot, xs, ysTop, swipeDuration); // Vuốt lên = Cuộn xuống
                                                                        }
                                                                        else
                                                                        {
                                                                            swipe(xs, ysTop, xs, ysBot, swipeDuration); // Vuốt xuống = Cuộn lên
                                                                        }
                                                                        delay(2500); // Chờ render ổn định màn hình sau khi vuốt
                                                                    }
                                                                }
                                                                break;
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

                                                                    checkMatrixActionLoop:
                                                                    while (true)
                                                                    {
                                                                        // 0. LẤY DATA MÀN HÌNH THÔNG MINH
                                                                        List<TextBlock> smartList = getCheckAnswerSmart();
                                                                        List<TextBlock> currentVisible = smartList.stream()
                                                                                .filter(x -> x.y > 180 && x.y < 2800).collect(Collectors.toList());

                                                                        // =======================================================
                                                                        // 1. TÌM TỌA ĐỘ HÀNG GỐC (BỌC THÉP CHỐNG CẮN NHẦM "AXIORY" VÀ "AXI")
                                                                        // =======================================================
                                                                        String cleanRowLabel = HSQTools.getOnlyTextLinq(normalizeText(rowLabel));

                                                                        HSQTools.TextBlock exactRowNode = currentVisible.stream()
                                                                                .filter(x -> {
                                                                                    String nodeTxt = HSQTools.getOnlyTextLinq(normalizeText(x.text));
                                                                                    if (nodeTxt.isEmpty()) return false;

                                                                                    // Ưu tiên 1: Khớp chuẩn xác 100%
                                                                                    if (nodeTxt.equals(cleanRowLabel)) return true;

                                                                                    // Ưu tiên 2: Khớp chứa (Contains) NHƯNG CÓ KIỂM SOÁT
                                                                                    if (nodeTxt.contains(cleanRowLabel)) {
                                                                                        int lenDiff = nodeTxt.length() - cleanRowLabel.length();
                                                                                        // 🔥 LUẬT TỬ HÌNH: Nếu từ khoá ngắn (<= 4 chữ, VD: Axi), cấm cắn nhầm từ dài (Axiory - dư 3 chữ)
                                                                                        if (cleanRowLabel.length() <= 4 && lenDiff > 1) return false;

                                                                                        // Với các từ dài hơn, cho phép dư một ít để bù trừ rác OCR
                                                                                        if (lenDiff <= (cleanRowLabel.length() * 0.5)) return true;
                                                                                    }

                                                                                    // Ưu tiên 3: Sai số Levenshtein (Cho phép sai 20%)
                                                                                    if (HSQTools.levenshtein(nodeTxt, cleanRowLabel) <= Math.max(1, (int)(cleanRowLabel.length() * 0.2))) return true;

                                                                                    return false;
                                                                                })
                                                                                // SẮP XẾP: Thằng nào khớp tuyệt đối (equals) phải được đẩy lên Top 1
                                                                                // Nếu đều là contains, thằng nào có độ dài gần với từ khoá gốc nhất sẽ lên Top!
                                                                                .sorted(Comparator.comparingInt((HSQTools.TextBlock x) -> {
                                                                                    String nodeTxt = HSQTools.getOnlyTextLinq(normalizeText(x.text));
                                                                                    return nodeTxt.equals(cleanRowLabel) ? 0 : 1;
                                                                                }).thenComparingInt(x -> Math.abs(HSQTools.getOnlyTextLinq(normalizeText(x.text)).length() - cleanRowLabel.length())))
                                                                                .findFirst().orElse(null);

                                                                        android.graphics.Point rowPt = (exactRowNode != null) ? new android.graphics.Point(exactRowNode.x, exactRowNode.y) : null;
                                                                        int targetY = (rowPt != null) ? rowPt.y : -1;

                                                                        // =======================================================
                                                                        // 🚀 XỬ LÝ VUỐT 2 CHIỀU (KHI KHÔNG TÌM THẤY HÀNG)
                                                                        // =======================================================
                                                                        if (rowPt == null)
                                                                        {
                                                                            if (HSQTools.areAlmostSame(tempCompare, currentVisible, 20))
                                                                            {
                                                                                if (vuotTimKiem == 0) {
                                                                                    vuotTimKiem = 1;
                                                                                    updateNotificationContent("Chạm đáy! Quay xe vuốt ngược lên tìm Hàng...");
                                                                                } else {
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

                                                                            if (vuotTimKiem == 0) {
                                                                                swipe(xCenter, yBot, xCenter, yTop, 1500); // Vuốt xuống
                                                                            } else {
                                                                                swipe(xCenter, yTop, xCenter, yBot, 1500); // Vuốt lên
                                                                            }
                                                                            delay(2000);
                                                                            continue checkMatrixActionLoop;
                                                                        }

                                                                        // =======================================================
                                                                        // 🚀 TÌM THẤY HÀNG -> ÉP VỊ TRÍ & PHÂN TÍCH XML
                                                                        // =======================================================
                                                                        // Nếu Hàng nằm sát đáy quá, đẩy nhẹ lên giữa màn hình để lộ XML bên dưới
                                                                        if (rowPt.y > 2300 && !swipeUp)
                                                                        {
                                                                            updateNotificationContent("Hàng nằm sát đáy, kéo lên giữa màn hình...");
                                                                            swipe(720, rowPt.y, 720, 1200, 1500);
                                                                            delay(2000);
                                                                            swipeUp = true;
                                                                            continue checkMatrixActionLoop;
                                                                        }

                                                                        int preciseY = rowPt.y;
                                                                        int titleRightX = 0;
                                                                        List<android.graphics.Rect> validRadioBoxes = new ArrayList<>();
                                                                        String currentXml = HSQTools.getFlexibleXML();

                                                                        try {
                                                                            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                            // BƯỚC A: Mò mẫm Y chuẩn (Quét trục Y để tìm tâm thật của lỗ Radio)
                                                                            for (int offset = -40; offset <= 160; offset += 40) {
                                                                                int testY = rowPt.y + offset;
                                                                                for (int i = 0; i < nodes.getLength(); i++) {
                                                                                    org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                                                                                    if (r != null && Math.abs(r.centerY() - testY) <= 40 && r.centerX() > (rowPt.x + 100)) {
                                                                                        if (r.width() > 20 && r.width() < 300 && r.height() > 20) {
                                                                                            preciseY = testY;
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }
                                                                                if (preciseY != rowPt.y) break;
                                                                            }

                                                                            // BƯỚC B: Tìm ranh giới bên phải của chữ Tiêu đề hàng (ĐÃ TÍCH HỢP BỌC THÉP CHỐNG CẮN NHẦM)
                                                                            for (int i = 0; i < nodes.getLength(); i++) {
                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                String nodeText = HSQTools.getOnlyTextLinq(normalizeText(node.getAttribute("text") + " " + node.getAttribute("content-desc")));

                                                                                if (!nodeText.isEmpty()) {
                                                                                    boolean isMatch = false;
                                                                                    if (nodeText.equals(cleanRowLabel)) {
                                                                                        isMatch = true;
                                                                                    } else if (nodeText.contains(cleanRowLabel)) {
                                                                                        int lenDiff = nodeText.length() - cleanRowLabel.length();
                                                                                        if (!(cleanRowLabel.length() <= 4 && lenDiff > 1) && lenDiff <= (cleanRowLabel.length() * 0.5)) {
                                                                                            isMatch = true;
                                                                                        }
                                                                                    }

                                                                                    if (isMatch) {
                                                                                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                        if (r != null && Math.abs(r.centerY() - preciseY) <= 40) {
                                                                                            titleRightX = r.right + 40; // Lề an toàn
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }

                                                                            if (titleRightX == 0 && rowPt != null) {
                                                                                titleRightX = rowPt.x + (rowLabel.length() * 25) + 50;
                                                                            }
                                                                            if (titleRightX > (widthOfScreen * 0.7)) titleRightX = (int)(widthOfScreen * 0.7);

                                                                            // BƯỚC C: Quét SẠCH SẼ các lỗ Radio THẬT (dựa vào Y chuẩn và X của ranh giới chữ)
                                                                            for (int i = 0; i < nodes.getLength(); i++) {
                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                                                                                if (r != null && Math.abs(r.centerY() - preciseY) <= 40 && r.centerX() > titleRightX) {
                                                                                    if (r.width() > 20 && r.width() < 300 && r.height() > 20) {
                                                                                        validRadioBoxes.add(r);
                                                                                    }
                                                                                }
                                                                            }
                                                                        } catch (Exception ignored) {}

                                                                        // =======================================================
                                                                        // 🚀 BƯỚC D: GỘP NODE TRÙNG (DEDUPLICATE)
                                                                        // =======================================================
                                                                        List<android.graphics.Rect> uniqueBoxes = new ArrayList<>();
                                                                        for (android.graphics.Rect r : validRadioBoxes) {
                                                                            boolean isDuplicate = false;
                                                                            for (android.graphics.Rect u : uniqueBoxes) {
                                                                                if (Math.abs(r.centerX() - u.centerX()) < 30) {
                                                                                    isDuplicate = true;
                                                                                    break;
                                                                                }
                                                                            }
                                                                            if (!isDuplicate) {
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
                                                                        if (parts.length >= 3) {
                                                                            try {
                                                                                totalColsExpected = Integer.parseInt(parts[2].trim());
                                                                            } catch (Exception ignored) {}
                                                                        }

                                                                        if (isNumericCol)
                                                                        {
                                                                            int targetColIndex = Integer.parseInt(colLabel) - 1;
                                                                            String numCacheKey = "MATRIX_COL_IDX_" + targetColIndex;

                                                                            // 1. NẾU CÓ XML XỊN -> Đâm trúng tim đen lỗ Radio
                                                                            if (!validRadioBoxes.isEmpty() && targetColIndex >= 0 && targetColIndex < validRadioBoxes.size()) {
                                                                                clickX = validRadioBoxes.get(targetColIndex).centerX();
                                                                                matrixColumnCache.put(numCacheKey, clickX);
                                                                                updateNotificationContent("XML Xịn: Bắt được Cột " + (targetColIndex + 1) + " tại X=" + clickX);
                                                                            }
                                                                            else
                                                                            {
                                                                                // 2. RÚT CACHE NẾU CÓ
                                                                                if (matrixColumnCache.containsKey(numCacheKey)) {
                                                                                    clickX = matrixColumnCache.get(numCacheKey);
                                                                                    updateNotificationContent("Rút Cache: Lấy mốc Cột " + (targetColIndex + 1) + " tại X=" + clickX);
                                                                                }
                                                                                else
                                                                                {
                                                                                    // =======================================================
                                                                                    // 🌟 3. FALLBACK TOÁN HỌC (TÍNH TOÁN DỰA TRÊN TỔNG CỘT TỪ KỊCH BẢN)
                                                                                    // =======================================================
                                                                                    // Nếu kịch bản CHƯA truyền đủ 3 tham số, Tool sẽ tự mò bằng Regex (Logic cũ)
                                                                                    if (parts.length < 3) {
                                                                                        try {
                                                                                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("~([^{}]+)\\}").matcher(tempTextAnswer);
                                                                                            int maxNumericCol = 0;
                                                                                            boolean isAllNumeric = true;
                                                                                            java.util.Set<String> uniqueTextCols = new java.util.HashSet<>();

                                                                                            while (m.find()) {
                                                                                                String valAfterTilde = m.group(1).trim();
                                                                                                uniqueTextCols.add(valAfterTilde);
                                                                                                if (valAfterTilde.matches("-?\\d+")) {
                                                                                                    int val = Integer.parseInt(valAfterTilde);
                                                                                                    if (val > maxNumericCol) maxNumericCol = val;
                                                                                                } else {
                                                                                                    isAllNumeric = false;
                                                                                                }
                                                                                            }

                                                                                            if (isAllNumeric && maxNumericCol > 0) totalColsExpected = maxNumericCol;
                                                                                            else if (!uniqueTextCols.isEmpty()) totalColsExpected = uniqueTextCols.size();
                                                                                        } catch (Exception e) {
                                                                                            totalColsExpected = 3;
                                                                                        }
                                                                                    }

                                                                                    // --- BẮT ĐẦU CHIA TỈ LỆ TOÁN HỌC ---
                                                                                    // 🌟 KHÁC BIỆT VỚI DRAGDROP: Matrix có Cột Tiêu Đề bên trái!
                                                                                    // Tool BẮT BUỘC phải bỏ qua Cột Tiêu đề này trước khi chia khoảng trống.

                                                                                    int startX = titleRightX > 0 ? titleRightX : (rowPt.x + (cleanRowLabel.length() * 25) + 30);

                                                                                    // Khóa mốc lề trái không vượt quá 60% màn hình
                                                                                    if (startX > (widthOfScreen * 0.6)) startX = (int)(widthOfScreen * 0.6);

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
                                                                            if (matrixColumnCache.containsKey(textCacheKey)) {
                                                                                headerX = matrixColumnCache.get(textCacheKey);
                                                                                updateNotificationContent("Universal Matrix: Rút Cache Cột Text [" + colLabel + "] tại X=" + headerX);
                                                                            }
                                                                            else
                                                                            {
                                                                                // Gemini nhả: {IC Markets~Chua su dung} -> cleanColLabel = chuasudung
                                                                                String cleanColLabel = HSQTools.getOnlyTextLinq(normalizeText(colLabel));
                                                                                final int finalPreciseY = preciseY;

                                                                                // 🌟 BƯỚC A: TÍNH TOÁN LỀ TRÁI AN TOÀN (Bỏ qua Tiêu đề hàng)
                                                                                int safeStartX = titleRightX > 0 ? titleRightX : (rowPt != null ? rowPt.x + 250 : 300);

                                                                                // 🌟 BƯỚC B: LỌC ỨNG VIÊN (Nằm bên phải lề & Nằm trên hàng Radio)
                                                                                List<TextBlock> candidates = currentVisible.stream()
                                                                                        .filter(t -> t.y < (finalPreciseY - 30) && t.x > safeStartX)
                                                                                        .collect(Collectors.toList());

                                                                                if (!candidates.isEmpty()) {
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
                                                                                    for (TextBlock t : actualHeaders) {
                                                                                        boolean added = false;
                                                                                        for (Integer colX : columnsMap.keySet()) {
                                                                                            if (Math.abs(t.x - colX) < 60) { // Sai số X nới lên 60px
                                                                                                columnsMap.get(colX).add(t);
                                                                                                added = true;
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                        if (!added) {
                                                                                            List<TextBlock> newList = new ArrayList<>();
                                                                                            newList.add(t);
                                                                                            columnsMap.put(t.x, newList);
                                                                                        }
                                                                                    }

                                                                                    // 🌟 BƯỚC F: SO KHỚP CHUỖI THÔNG MINH
                                                                                    for (Map.Entry<Integer, List<TextBlock>> entry : columnsMap.entrySet()) {
                                                                                        List<TextBlock> cluster = entry.getValue();
                                                                                        cluster.sort(Comparator.comparingInt(t -> t.y)); // Xếp Y từ trên xuống

                                                                                        StringBuilder combinedTextBuilder = new StringBuilder();
                                                                                        for (TextBlock t : cluster) {
                                                                                            combinedTextBuilder.append(t.text).append(" ");
                                                                                        }
                                                                                        String combinedText = HSQTools.getOnlyTextLinq(normalizeText(combinedTextBuilder.toString()));

                                                                                        boolean isMatch = false;

                                                                                        // 1. Khớp tuyệt đối
                                                                                        if (combinedText.equals(cleanColLabel)) {
                                                                                            isMatch = true;
                                                                                        }
                                                                                        // 2. Khớp chứa (Chỉ áp dụng cho từ dài > 3 ký tự, cấm áp dụng cho "co" để tránh cắn nhầm)
                                                                                        else if (cleanColLabel.length() > 3 && combinedText.contains(cleanColLabel)) {
                                                                                            int lenDiff = combinedText.length() - cleanColLabel.length();
                                                                                            if (lenDiff <= (cleanColLabel.length() * 0.5)) isMatch = true;
                                                                                        }
                                                                                        // 3. Sai số Levenshtein (CỨU CÁNH CHO CHỮ "CO")
                                                                                        // Từ 1-4 ký tự cho phép sai 1 lỗi (Co -> c6 vẫn bắt được)
                                                                                        else {
                                                                                            int allowedErr = Math.max(1, (int)(cleanColLabel.length() * 0.25));
                                                                                            if (HSQTools.levenshtein(combinedText, cleanColLabel) <= allowedErr) {
                                                                                                isMatch = true;
                                                                                            }
                                                                                        }

                                                                                        if (isMatch) {
                                                                                            headerX = entry.getKey();
                                                                                            updateNotificationContent("OCR Header: Bắt trúng cột [" + colLabel + "] tại X=" + headerX);
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }

                                                                                if (headerX != -1) {
                                                                                    matrixColumnCache.put(textCacheKey, headerX);
                                                                                }
                                                                            }

                                                                            if (headerX != -1) {
                                                                                clickX = headerX;
                                                                                // =======================================================
                                                                                // 🚀 SMART SNAPPING
                                                                                // =======================================================
                                                                                if (!validRadioBoxes.isEmpty()) {
                                                                                    final int finalHeaderX = headerX;
                                                                                    android.graphics.Rect closestBox = validRadioBoxes.stream()
                                                                                            .filter(r -> Math.abs(r.centerX() - finalHeaderX) <= 180) // Nới rộng snap
                                                                                            .min(Comparator.comparingInt(r -> Math.abs(r.centerX() - finalHeaderX)))
                                                                                            .orElse(null);

                                                                                    if (closestBox != null) {
                                                                                        clickX = closestBox.centerX();
                                                                                        updateNotificationContent("Snap! Hít Text-Column vào tâm lỗ tại X=" + clickX);
                                                                                    }
                                                                                } else {
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
                                                                                continue checkMatrixActionLoop;
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
                                                                                if (clickChoose == 0)
                                                                                {
                                                                                    previousText = step;
                                                                                    currentState = STATE_ROLLBACK1;
                                                                                    tempTextAnswer = textAnswer = "begin|swipemore|1|end";
                                                                                    continue stateMachine;
                                                                                }
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
                                                                            List<TextBlock> currentVisible = currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList());

                                                                            // 2. Dùng mắt thần tìm Tâm Thẻ Cần Kéo
                                                                            android.graphics.Point sourcePt = HSQTools.smartFindTextPoint(sourceStr);
                                                                            android.graphics.Point targetPt = null;

                                                                            // 3. 🌟 TOÁN HỌC THUẦN TÚY (PURE MATH) - NHANH, GỌN, CHÍNH XÁC 100%
                                                                            if (isNumericTarget) {
                                                                                int targetColIndex = Integer.parseInt(targetStr) - 1;

                                                                                // Ép ranh giới an toàn lỡ AI ngáo
                                                                                if (targetColIndex < 0) targetColIndex = 0;
                                                                                if (targetColIndex >= totalColsExpected) targetColIndex = totalColsExpected - 1;

                                                                                // Cắt màn hình ra làm N phần bằng nhau
                                                                                int stepX = widthOfScreen / Math.max(1, totalColsExpected);

                                                                                // Đâm thẳng Tâm X vào giữa cái cột được chọn
                                                                                int dropX = (targetColIndex * stepX) + (stepX / 2);

                                                                                // Fix cứng Y ném vào 70% màn hình
                                                                                int dropY = (int) (heightOfScreen * 0.7);

                                                                                targetPt = new android.graphics.Point(dropX, dropY);
                                                                            } else {
                                                                                targetPt = HSQTools.smartFindTextPoint(targetStr);
                                                                            }

                                                                            // 4. THỰC THI VUỐT
                                                                            if (sourcePt != null && targetPt != null)
                                                                            {
                                                                                updateNotificationContent("Kéo [" + sourceStr + "] -> X=" + targetPt.x + ", Y=" + targetPt.y);
                                                                                swipe(sourcePt.x, sourcePt.y, targetPt.x, targetPt.y, 1500);
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
                                                                                    swipe(xCenter, yBot, xCenter, yTop, 1500);
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
                                                            int vuotTimKiem = 0;
                                                            checkDropdownSmartLoop:
                                                            while (true)
                                                            {
                                                                Matcher match = Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                                if (match.find())
                                                                {
                                                                    String contextStr = match.group(1);
                                                                    updateNotificationContent("Smart Dropdown: " + contextStr);

                                                                    List<TextBlock> currentScreen = getCheckAnswerSmart();
                                                                    int vuotLenLai = 0, davuotlen = 0;

                                                                    checkDropdownActionLoop:
                                                                    while (true)
                                                                    {
                                                                        // ========================================================
                                                                        // 🎯 THUẬT TOÁN TÌM MỐC SIÊU CẤP: DIỆT TIÊU ĐỀ ẢO GIÁC
                                                                        // ========================================================
                                                                        String normTarget = HSQTools.getOnlyTextLinq(normalizeText(contextStr));
                                                                        HSQTools.TextBlock exactTextNode = currentScreen.stream()
                                                                                .filter(x -> x.y > 180)
                                                                                .filter(x ->
                                                                                {
                                                                                    String cleanText = HSQTools.getOnlyTextLinq(normalizeText(x.text));
                                                                                    if (cleanText == null || cleanText.isEmpty()) return false;

                                                                                    // Điều kiện 1: Chữ trên màn hình CHỨA từ khóa của Gemini (Bình thường, an toàn)
                                                                                    if (cleanText.contains(normTarget)) return true;

                                                                                    // Điều kiện 2: Từ khóa của Gemini CHỨA chữ trên màn hình (Dùng khi OCR đọc thiếu chữ)
                                                                                    // 🔥 BỌC THÉP TỬ HÌNH: Ép chiều dài >= 5 để cấm nhặt rác ("a", "i", "o", "nu"...)
                                                                                    if (normTarget.contains(cleanText) && cleanText.length() >= 5) return true;

                                                                                    return false;
                                                                                })
                                                                                // Sắp xếp: Thằng nào bằng tuyệt đối lên đầu, rồi đến thằng nào có độ dài NGẮN NHẤT
                                                                                .sorted(Comparator.comparingInt((HSQTools.TextBlock x) ->
                                                                                {
                                                                                    String cleanText = HSQTools.getOnlyTextLinq(normalizeText(x.text));
                                                                                    return cleanText.equals(normTarget) ? 0 : 1;
                                                                                }).thenComparingInt(x -> Math.abs(x.text.length() - contextStr.length())))
                                                                                .findFirst().orElse(null);

                                                                        if (exactTextNode == null)
                                                                        {
                                                                            if (vuotLenLai < 2)
                                                                            {
                                                                                swipe(xCenter, yBot, xCenter, yTop, 1500);
                                                                                delay(2000);
                                                                                currentScreen = getCheckAnswerSmart();
                                                                                vuotLenLai++;
                                                                                continue checkDropdownActionLoop;
                                                                            }
                                                                            handleActionFailure(
                                                                                    "Dropdown", step, currentScreen,
                                                                                    "Mày đưa lệnh [" + step + "] nhưng tao cuộn nát máy không thấy chữ này đâu, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời",
                                                                                    splitTextAnswer[1]
                                                                            );
                                                                            tempTextAnswer = textAnswer;
                                                                            continue stateMachine;
                                                                        }
                                                                        if (exactTextNode.y > 2600 && davuotlen == 0) {
                                                                            updateNotificationContent("Câu hỏi nằm sát đáy, vuốt lên một chút để lộ Dropdown!");
                                                                            // Vuốt ngược lên trên một đoạn ngắn để đẩy câu hỏi lên giữa màn hình
                                                                            swipe(xCenter, yBot, xCenter, yBot - 600, 1000);
                                                                            delay(2000);
                                                                            currentScreen = getCheckAnswerSmart();
                                                                            davuotlen++;
                                                                            continue checkDropdownActionLoop; // Quét lại tọa độ cho chuẩn
                                                                        }
                                                                        // DANH SÁCH ỨNG VIÊN CLICK (THEO THỨ TỰ ƯU TIÊN GIẢM DẦN)
                                                                        List<android.graphics.Point> candidates = new ArrayList<>();

                                                                        // =========================================================
                                                                        // 🎯 TẦNG 1: QUÉT SẠCH SẼ BẰNG MẮT THẦN (OCR) - BẮT CHỮ "CHỌN MỘT"
                                                                        // Đây là phương án chính xác nhất vì nó nhìn thấy gì thì chọt nấy!
                                                                        // =========================================================
                                                                        HSQTools.TextBlock ocrDropdown = currentScreen.stream()
                                                                                .filter(n -> n.y > exactTextNode.y && n.y < exactTextNode.y + 700)
                                                                                .filter(n -> {
                                                                                    String c = HSQTools.getOnlyTextLinq(normalizeText(n.text));
                                                                                    // 🔥 BỔ SUNG: "luachon", "vuilongluachon"
                                                                                    return c.equals("v") || c.equals("chon") || c.equals("select") || c.equals("choose")
                                                                                            || c.equals("chonmot") || c.equals("selectone") || c.equals("vuilongchon")
                                                                                            || c.equals("luachon") || c.equals("vuilongluachon");
                                                                                })
                                                                                .min(Comparator.comparingInt(n -> n.y))
                                                                                .orElse(null);

                                                                        if (ocrDropdown != null) {
                                                                            updateNotificationContent("OCR Bắt sống chữ Chọn/Lựa Chọn tại Y=" + ocrDropdown.y);
                                                                            candidates.add(new android.graphics.Point(ocrDropdown.x, ocrDropdown.y));
                                                                        }

                                                                        // =========================================================
                                                                        // 🎯 TẦNG 2: BẮT NODE TÀNG HÌNH BẰNG XML (SIBLING NODE)
                                                                        // Dùng khi WebView giấu chữ "Vui lòng chọn" nhưng hở ra cái khung ô vuông
                                                                        // =========================================================
                                                                        if (candidates.isEmpty()) {
                                                                            String currentXml = HSQTools.getFlexibleXML();
                                                                            try {
                                                                                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                boolean foundAnchor = false;
                                                                                int anchorBottomY = exactTextNode.y;

                                                                                for (int i = 0; i < nodes.getLength(); i++) {
                                                                                    org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);

                                                                                    if (!foundAnchor) {
                                                                                        String text = node.getAttribute("text");
                                                                                        String desc = node.getAttribute("content-desc");
                                                                                        String combined = HSQTools.getOnlyTextLinq(normalizeText(text + " " + desc));

                                                                                        // Phải check cả độ dài để tránh tóm nhầm node cha bao trùm cả form
                                                                                        if (!combined.isEmpty() && (combined.contains(normTarget) || normTarget.contains(combined))) {
                                                                                            android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                            if (r != null && r.height() < 500) {
                                                                                                foundAnchor = true;
                                                                                                anchorBottomY = r.bottom;
                                                                                            }
                                                                                        }
                                                                                    } else {
                                                                                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                                                                                        if (r != null && r.top >= anchorBottomY - 20 && r.top < anchorBottomY + 500 && r.height() > 40) {

                                                                                            String text = node.getAttribute("text").toLowerCase();
                                                                                            String desc = node.getAttribute("content-desc").toLowerCase();
                                                                                            String combined = text + " " + desc;
                                                                                            String cleanCombined = HSQTools.getOnlyTextLinq(normalizeText(combined));

                                                                                            if (combined.contains("đáp án") || combined.contains("answer") ||
                                                                                                    combined.contains("câu trả lời") || combined.contains("lưu ý") || cleanCombined.length() > 25) {
                                                                                                continue;
                                                                                            }

                                                                                            // 🔥 BỔ SUNG: Chứa chữ "lựa chọn"
                                                                                            boolean hasTextMatch = combined.contains("chọn một") || combined.contains("select") || combined.contains("choose") || combined.contains("vui lòng chọn") || combined.contains("lựa chọn");

                                                                                            // 🔥 BỌC THÉP NÚT NEXT: Ô Dropdown TỐI THIỂU phải rộng 400px!
                                                                                            // Nút Next màu xanh thường là hình vuông hẹp chiều ngang (< 300px), loại nó ra ngay!
                                                                                            boolean isEmptyContainer = combined.trim().isEmpty() && r.height() > 60 && r.height() < 250 && r.width() > 400 && r.width() < 1200;
                                                                                            boolean isClickableView = node.getAttribute("clickable").equals("true") || node.getAttribute("class").contains("Spinner") || node.getAttribute("class").contains("Button");

                                                                                            if (hasTextMatch || (isEmptyContainer && isClickableView)) {
                                                                                                int safeX = r.width() > 800 ? r.left + 200 : r.centerX();
                                                                                                updateNotificationContent("XML Bắt được ô Dropdown ẩn tại Y=" + r.centerY());
                                                                                                candidates.add(new android.graphics.Point(safeX, r.centerY()));
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            } catch (Exception ignored) {}
                                                                        }

                                                                        // =========================================================
                                                                        // 🎯 TẦNG 3: FALLBACK ASBL
                                                                        // =========================================================
                                                                        if (candidates.isEmpty()) {
                                                                            AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
                                                                            if (root != null) {
                                                                                android.graphics.Point asblPt = findDropdownNearByASBL(root, exactTextNode.x, exactTextNode.y, contextStr);
                                                                                if (asblPt != null) candidates.add(asblPt);
                                                                                root.recycle();
                                                                            }
                                                                        }

                                                                        // =========================================================
                                                                        // 🎯 TẦNG 4: THUẬT TOÁN SONG SINH (TWIN NODE - CUỐI BẢNG)
                                                                        // Đẩy xuống bét bảng để chỉ dùng khi vã quá, và khóa biên độ Y lại!
                                                                        // =========================================================
                                                                        if (candidates.isEmpty()) {
                                                                            List<HSQTools.TextBlock> twinNodes = currentScreen.stream()
                                                                                    .filter(x -> x.y > exactTextNode.y && x.y < exactTextNode.y + 600) // Khóa: Chỉ tìm ruột thừa quanh bán kính 600px!
                                                                                    .filter(x -> {
                                                                                        String cleanText = HSQTools.getOnlyTextLinq(normalizeText(x.text));
                                                                                        return !cleanText.isEmpty() && (cleanText.contains(normTarget) || normTarget.contains(cleanText));
                                                                                    })
                                                                                    .collect(Collectors.toList());

                                                                            if (twinNodes.size() > 0) {
                                                                                twinNodes.sort((a, b) -> Integer.compare(b.y, a.y));
                                                                                HSQTools.TextBlock dropdownNode = twinNodes.get(0);
                                                                                updateNotificationContent("Dùng phao cứu sinh Twin Node tại Y=" + dropdownNode.y);
                                                                                candidates.add(new android.graphics.Point(dropdownNode.x, dropdownNode.y));
                                                                            }
                                                                        }

                                                                        // THỰC THI CHIẾN THUẬT THỬ SAI LỲ LỢM
                                                                        boolean isOpened = false;
                                                                        for (android.graphics.Point pt : candidates)
                                                                        {
                                                                            updateNotificationContent("Chọt tọa độ: " + pt.x + "," + pt.y);
                                                                            delay(800);
                                                                            click(pt.x, pt.y, false);
                                                                            delay(4500); // Chờ list xổ ra

                                                                            List<TextBlock> afterClick = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                                            if (!HSQTools.areAlmostSame(currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList()), afterClick, 20))
                                                                            {
                                                                                isOpened = true;
                                                                                break; // List mở thành công, quay xe!
                                                                            }
                                                                        }

                                                                        if (isOpened)
                                                                        {
                                                                            if (checkGDKS == 9)
                                                                            {
                                                                                break checkDropdownSmartLoop;
                                                                            }
                                                                            if (clickChoose == 0)
                                                                            {
                                                                                previousText = step;
                                                                                clickChoose = 0;
                                                                                tempSwipeCount = 0;
                                                                                tempTextAnswer = textAnswer = "begin|swipemore|1|end";
                                                                                currentState = STATE_ROLLBACK1;
                                                                                continue stateMachine;
                                                                            }
                                                                            else
                                                                            {
                                                                                clickChoose = 0;
                                                                                previousText = "";
                                                                                p--;
                                                                            }
                                                                            break checkDropdownSmartLoop;
                                                                        }
                                                                        else
                                                                        {
                                                                            if (vuotTimKiem == 0) {
                                                                                updateNotificationContent("Không thấy Dropdown. Vuốt XUỐNG tìm kiếm...");
                                                                                swipe(xCenter, yBot, xCenter, yTop, swipeDuration);
                                                                                delay(2000);

                                                                                List<TextBlock> afterSwipe = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());

                                                                                // Nếu vuốt xuống mà màn hình KHÔNG đổi (chạm đáy cmnr) -> Chuyển sang vuốt lên!
                                                                                if (HSQTools.areAlmostSame(currentScreen, afterSwipe, 20)) {
                                                                                    vuotTimKiem++; // Sang trạng thái vuốt ngược
                                                                                    updateNotificationContent("Chạm đáy! Quay xe vuốt ngược LÊN tìm kiếm...");
                                                                                    swipe(xCenter, yTop, xCenter, yBot, swipeDuration);
                                                                                    delay(2000);
                                                                                }

                                                                                currentScreen = getCheckAnswerSmart();
                                                                                continue checkDropdownActionLoop;

                                                                            } else if (vuotTimKiem == 1) {
                                                                                updateNotificationContent("Đang vuốt LÊN tìm kiếm...");
                                                                                swipe(xCenter, yTop, xCenter, yBot, swipeDuration);
                                                                                delay(2000);

                                                                                List<TextBlock> afterSwipe = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());

                                                                                // Nếu vuốt lên mà màn hình KHÔNG đổi (chạm đỉnh cmnr) -> Toang, đéo thấy gì cả!
                                                                                if (HSQTools.areAlmostSame(currentScreen, afterSwipe, 20)) {
                                                                                    vuotTimKiem++; // Sang trạng thái chết
                                                                                } else {
                                                                                    currentScreen = getCheckAnswerSmart();
                                                                                    continue checkDropdownActionLoop;
                                                                                }
                                                                            }
                                                                            handleActionFailure(
                                                                                    "Dropdown", step, currentScreen,
                                                                                    "Tao đã thử chọt hết các ứng viên của [" + step + "] nhưng màn hình không đổi, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời",
                                                                                    splitTextAnswer[1]
                                                                            );
                                                                            tempTextAnswer = textAnswer;
                                                                            continue stateMachine;
                                                                        }
                                                                    }
                                                                }
                                                                break checkDropdownSmartLoop;
                                                            }

                                                            if (checkGDKS == 9)
                                                            {
                                                                continue;
                                                            }
                                                        }
                                                        else if (step.contains("click_index"))
                                                        {
                                                            Matcher match = java.util.regex.Pattern.compile("\\{(\\d+)\\}").matcher(step);
                                                            if (match.find())
                                                            {
                                                                int targetIndex = Integer.parseInt(match.group(1));
                                                                int currentGlobalCount = 0;
                                                                int vuotLenLai = 0;
                                                                int ignoreYLimit = 300;
                                                                List<HSQTools.TextBlock> tempCompare = new ArrayList<>();

                                                                while (true)
                                                                {
                                                                    List<android.graphics.Rect> clickableRegions = new ArrayList<>();
                                                                    final int currentIgnoreYLimit = ignoreYLimit;

                                                                    // 1. TẦNG 1 & 2: DÙNG ASBL VÀ XML TÌM CHECKBOX/RADIO CHUẨN
                                                                    AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
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

                                                                    // 2. TẦNG 3: THỦ MÔN OCR (FALLBACK KHI WEBVIEW GIẤU CLASS)
                                                                    // Nếu cả 2 thằng trên đều mù, ta dùng OCR tìm các dòng text để dóng hàng ngang
                                                                    if (clickableRegions.isEmpty())
                                                                    {
                                                                        updateNotificationContent("Dùng OCR dóng hàng tìm ô thứ " + targetIndex);
                                                                        List<TextBlock> ocrNodes = getOcrTextBlocks();
                                                                        // Lọc lấy các node text nằm ở nửa trái màn hình (thường là bắt đầu của 1 option)
                                                                        for (TextBlock node : ocrNodes)
                                                                        {
                                                                            if (node.y > currentIgnoreYLimit && node.y < 2800 && node.x < 1000)
                                                                            {
                                                                                // Tạo một Rect giả định nằm bên trái đoạn text 50px
                                                                                clickableRegions.add(new android.graphics.Rect(node.x - 100, node.y - 30, node.x - 20, node.y + 30));
                                                                            }
                                                                        }
                                                                    }

                                                                    // Sắp xếp các vùng tìm thấy từ trên xuống dưới
                                                                    clickableRegions.sort(Comparator.comparingInt(r -> r.top));

                                                                    // 3. THỰC THI CLICK HOẶC VUỐT TÌM TIẾP
                                                                    if (!clickableRegions.isEmpty())
                                                                    {
                                                                        if (targetIndex <= currentGlobalCount + clickableRegions.size())
                                                                        {
                                                                            int localIndex = targetIndex - currentGlobalCount - 1;
                                                                            if (localIndex >= 0 && localIndex < clickableRegions.size())
                                                                            {
                                                                                android.graphics.Rect targetRect = clickableRegions.get(localIndex);
                                                                                click(targetRect.centerX(), targetRect.centerY(), false);
                                                                                delay(2000);
                                                                                break; // Thành công
                                                                            }
                                                                        }

                                                                        // Nếu chưa tới index cần tìm -> Vuốt màn hình
                                                                        currentGlobalCount += clickableRegions.size();
                                                                        int lastY = clickableRegions.get(clickableRegions.size() - 1).top;
                                                                        tempCompare = getCheckAnswerSmart(); // Lưu lại để check kẹt

                                                                        updateNotificationContent("Đã đếm " + currentGlobalCount + " ô. Vuốt tiếp...");
                                                                        swipe(720, lastY, 720, 400, 1500);
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
                                                            checkAccordionSmartLoop:
                                                            while (true)
                                                            {
                                                                Matcher match = java.util.regex.Pattern.compile("\\{([^}]+)\\}").matcher(step);
                                                                if (match.find() && match.group(1).contains("^"))
                                                                {
                                                                    String innerContent = match.group(1);
                                                                    String[] sections = innerContent.split("\\^");

                                                                    for (String section : sections)
                                                                    {
                                                                        if (section == null || section.trim().isEmpty())
                                                                            continue;
                                                                        String[] headerAndItems = section.split(":");
                                                                        if (headerAndItems.length == 2)
                                                                        {
                                                                            String headerStr = headerAndItems[0];
                                                                            String[] itemsToClick = headerAndItems[1].split(",");

                                                                            updateNotificationContent("Smart Accordion: " + headerStr);
                                                                            List<HSQTools.TextBlock> tempCompare = new ArrayList<>();
                                                                            int vuotLenLaiHeader = 0;

                                                                            while (true)
                                                                            {
                                                                                // 1. TÌM TỌA ĐỘ HEADER BẰNG SMART SEARCH
                                                                                android.graphics.Point hPt = HSQTools.smartFindTextPoint(headerStr);
                                                                                List<TextBlock> currentScreen = getCheckAnswerSmart();

                                                                                if (hPt != null)
                                                                                {
                                                                                    // ĐƯA HEADER RA GIỮA MÀN CHO DỄ XỬ LÝ
                                                                                    if (hPt.y > 2300)
                                                                                    {
                                                                                        swipe(720, 2200, 720, 1200, 1500);
                                                                                        delay(2000);
                                                                                        currentScreen = getCheckAnswerSmart();
                                                                                        hPt = HSQTools.smartFindTextPoint(headerStr);
                                                                                    }

                                                                                    // 2. KIỂM TRA XEM ACCORDION ĐÃ MỞ CHƯA
                                                                                    boolean isAlreadyOpen = false;
                                                                                    if (itemsToClick.length > 0)
                                                                                    {
                                                                                        String firstItem = itemsToClick[0];
                                                                                        final int headerY = hPt.y;
                                                                                        // Nếu thấy Item xuất hiện ngay bên dưới Header (trong khoảng 800px) -> Đã mở
                                                                                        isAlreadyOpen = currentScreen.stream().anyMatch(x ->
                                                                                                normalizeText(x.text).contains(normalizeText(firstItem)) && x.y > headerY && x.y < headerY + 800);
                                                                                    }

                                                                                    // 3. NẾU CHƯA MỞ -> CLICK ĐỂ MỞ
                                                                                    if (!isAlreadyOpen)
                                                                                    {
                                                                                        AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
                                                                                        android.graphics.Point clickPt = findClickableParentByASBL(root, hPt.x, hPt.y);
                                                                                        if (root != null)
                                                                                            root.recycle();

                                                                                        if (clickPt != null)
                                                                                            click(clickPt.x, clickPt.y, false);
                                                                                        else
                                                                                            click(hPt.x, hPt.y, false);
                                                                                        delay(3000);
                                                                                        currentScreen = getCheckAnswerSmart();
                                                                                    }

                                                                                    // 4. TÌM VÀ CLICK CÁC ITEM BÊN TRONG
                                                                                    for (String item : itemsToClick)
                                                                                    {
                                                                                        updateNotificationContent("Click Item: " + item);
                                                                                        int itemVuot = 0;
                                                                                        while (itemVuot < 3)
                                                                                        {
                                                                                            android.graphics.Point iPt = HSQTools.smartFindTextPoint(item);
                                                                                            if (iPt != null)
                                                                                            {
                                                                                                click(iPt.x, iPt.y, false);
                                                                                                delay(2000);
                                                                                                break;
                                                                                            }
                                                                                            swipe(720, 2200, 720, 1200, 1500);
                                                                                            delay(2000);
                                                                                            itemVuot++;
                                                                                        }
                                                                                    }
                                                                                    break; // Xong một Section
                                                                                }

                                                                                // --- KIỂM TRA LỖI & BÁO TELEGRAM (CHO HEADER) ---
                                                                                List<TextBlock> visible = currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                                                if (hPt == null || HSQTools.areAlmostSame(tempCompare, visible, 20))
                                                                                {
                                                                                    if (vuotLenLaiHeader < 2)
                                                                                    {
                                                                                        tempCompare = visible;
                                                                                        swipe(xCenter, yBot, xCenter, yTop, 1500);
                                                                                        delay(2000);
                                                                                        vuotLenLaiHeader++;
                                                                                        continue;
                                                                                    }

                                                                                    // THỰC SỰ LỖI: GỬI TELEGRAM VIP
                                                                                    handleActionFailure(
                                                                                            "Accordion", step, getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList()),
                                                                                            "Tao không thấy tiêu đề Accordion [" + headerStr + "]. Mày check lại xem nó có bị viết sai không?, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời",
                                                                                            splitTextAnswer[1]
                                                                                    );
                                                                                    tempTextAnswer = textAnswer;
                                                                                    continue stateMachine;
                                                                                }
                                                                                tempCompare = visible;
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                break;
                                                            }
                                                        }
                                                        else if (step.contains("click_block"))
                                                        {
                                                            checkBlockSmartLoop:
                                                            while (true)
                                                            {
                                                                Matcher match = java.util.regex.Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                                if (match.find() && match.group(1).contains("~"))
                                                                {
                                                                    String[] parts = match.group(1).split("~");
                                                                    String headerStr = parts[0];
                                                                    String answerStr = parts[1];

                                                                    updateNotificationContent("Smart Block: " + headerStr + " -> " + answerStr);
                                                                    List<HSQTools.TextBlock> tempCompare = new ArrayList<>();
                                                                    int vuotLenLai = 0;
                                                                    int keoHeader = 0; // Đếm số lần kéo Header để chống kẹt đáy

                                                                    boolean daThayHeader = false;

                                                                    checkBlockActionLoop:
                                                                    while (true)
                                                                    {
                                                                        // 0. LẤY DỮ LIỆU MÀN HÌNH THÔNG MINH
                                                                        List<HSQTools.TextBlock> smartList = getCheckAnswerSmart();
                                                                        List<HSQTools.TextBlock> currentVisible = smartList.stream()
                                                                                .filter(x -> x.y > 180 && x.y < 2800).collect(Collectors.toList());

                                                                        // =======================================================
                                                                        // 1. TÌM TIÊU ĐỀ CÂU HỎI (HEADER) - LAI GHÉP OCR & XML
                                                                        // =======================================================
                                                                        int targetY = -1;
                                                                        String cleanHeader = normalizeText(headerStr).replaceAll("[^a-z0-9]", "");

                                                                        // TÌM BẰNG OCR TRƯỚC
                                                                        HSQTools.TextBlock foundHeader = currentVisible.stream()
                                                                                .filter(x ->
                                                                                {
                                                                                    String nodeTxt = normalizeText(x.text).replaceAll("[^a-z0-9]", "");
                                                                                    if (nodeTxt.isEmpty()) return false;

                                                                                    // Tầng 1: Khớp tuyệt đối 100%
                                                                                    if (nodeTxt.equals(cleanHeader)) return true;

                                                                                    // Tầng 2: Node chứa Header (Ví dụ tìm Axi, chống cắn nhầm Axiory)
                                                                                    if (nodeTxt.contains(cleanHeader)) {
                                                                                        int lenDiff = nodeTxt.length() - cleanHeader.length();
                                                                                        if (cleanHeader.length() <= 4 && lenDiff > 1) return false;
                                                                                        if (lenDiff <= (cleanHeader.length() * 0.5)) return true;
                                                                                    }

                                                                                    // Tầng 3: Header chứa Node (Khóa tử hình lỗi tìm Axiory cắn Axi)
                                                                                    // Chỉ cho phép nếu chữ quét được đủ dài và gần bằng Header
                                                                                    if (cleanHeader.contains(nodeTxt)) {
                                                                                        int lenDiff = cleanHeader.length() - nodeTxt.length();
                                                                                        if (nodeTxt.length() > 3 && lenDiff <= 2) return true;
                                                                                    }

                                                                                    // Tầng 4: Sai số Levenshtein toàn bộ
                                                                                    if (HSQTools.levenshtein(nodeTxt, cleanHeader) <= Math.max(1, (int)(cleanHeader.length() * 0.2))) return true;

                                                                                    // Tầng 5: Cắt tiền tố (Prefix matching) - Dành cho câu hỏi cõng giải thích
                                                                                    if (nodeTxt.length() >= cleanHeader.length()) {
                                                                                        String prefix = nodeTxt.substring(0, cleanHeader.length());
                                                                                        if (HSQTools.levenshtein(prefix, cleanHeader) <= Math.max(1, (int)(cleanHeader.length() * 0.2))) return true;
                                                                                    }

                                                                                    return false;
                                                                                })
                                                                                // 🌟 THUẬT TOÁN SẮP XẾP MỚI:
                                                                                // Ưu tiên 1: Chữ nào khớp chính xác 100% thì đưa lên Top 1.
                                                                                // Ưu tiên 2: Nếu cùng độ chính xác, ưu tiên chữ nằm gần đỉnh màn hình nhất (Y nhỏ nhất).
                                                                                .sorted(Comparator.comparingInt((HSQTools.TextBlock x) -> {
                                                                                    String nodeTxt = normalizeText(x.text).replaceAll("[^a-z0-9]", "");
                                                                                    return nodeTxt.equals(cleanHeader) ? 0 : 1;
                                                                                }).thenComparingInt(x -> x.y))
                                                                                .findFirst().orElse(null);

                                                                        if (foundHeader != null)
                                                                        {
                                                                            targetY = foundHeader.y;
                                                                            daThayHeader = true;
                                                                        }
                                                                        else
                                                                        {
                                                                            // 🔥 BỌC THÉP: Vớt Header bằng XML nếu OCR mù do chữ dính chùm
                                                                            String xml = HSQTools.getFlexibleXML();
                                                                            try {
                                                                                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                for (int i = 0; i < nodes.getLength(); i++) {
                                                                                    org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                    String rawText = node.getAttribute("text") + " " + node.getAttribute("content-desc");
                                                                                    String nodeTxt = normalizeText(rawText).replaceAll("[^a-z0-9]", "");

                                                                                    if (!nodeTxt.isEmpty() && (nodeTxt.equals(cleanHeader) || nodeTxt.contains(cleanHeader))) {
                                                                                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                        // Phải là cái Box không quá bự để tránh bắt nhầm cái Container
                                                                                        if (r != null && r.width() < (widthOfScreen * 0.8) && r.centerY() > 180 && r.centerY() < 2800) {
                                                                                            targetY = r.centerY();
                                                                                            daThayHeader = true;
                                                                                            updateNotificationContent("OCR Mù! Vớt được Header [" + headerStr + "] trong XML tại Y=" + targetY);
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }
                                                                            } catch (Exception ignored) {}
                                                                        }

                                                                        // =======================================================
                                                                        // 2. KÉO HEADER LÊN ĐỈNH ĐỂ LỘ ĐÁP ÁN (CHỐNG KẸT ĐÁY)
                                                                        // =======================================================
                                                                        if (targetY > 2300 && keoHeader < 2)
                                                                        {
                                                                            updateNotificationContent("Header " + headerStr + " nằm thấp, túm cổ kéo lên đỉnh...");
                                                                            swipe(xs, targetY, xs, 400, 2000);
                                                                            delay(2500);
                                                                            keoHeader++;
                                                                            continue checkBlockActionLoop;
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
                                                                            boolean isNumeric = answerStr.matches("\\d+");
                                                                            final int finalTargetY = targetY; // Biến Final chống lỗi Lambda

                                                                            if (isNumeric)
                                                                            {
                                                                                // CHIẾN THUẬT 1: TÌM THEO TEXT SỐ (Ví dụ click_block {Cau 1~5})
                                                                                String cleanAns = answerStr;
                                                                                HSQTools.TextBlock ansNode = currentVisible.stream()
                                                                                        .filter(x ->
                                                                                        {
                                                                                            if (finalTargetY != -1) return x.y > finalTargetY + 20 && x.y < finalTargetY + 1500;
                                                                                            return x.y > 180;
                                                                                        })
                                                                                        .filter(x ->
                                                                                        {
                                                                                            String nodeTxt = normalizeText(x.text).replaceAll("[^a-z0-9]", "");
                                                                                            return nodeTxt.equals(cleanAns) || nodeTxt.startsWith(cleanAns);
                                                                                        })
                                                                                        // VỚI SỐ THÌ ƯU TIÊN GẦN NHAU THEO Y
                                                                                        .min(Comparator.comparingInt(x -> (finalTargetY != -1) ? Math.abs(x.y - finalTargetY) : x.y))
                                                                                        .orElse(null);

                                                                                if (ansNode != null)
                                                                                {
                                                                                    finalClickPt = new android.graphics.Point(ansNode.x, ansNode.y);
                                                                                }
                                                                                else
                                                                                {
                                                                                    // CHIẾN THUẬT 2: TÌM SỐ ẨN TRONG HÀNG NGANG (MATRIX KIỂU CŨ)
                                                                                    String xml = HSQTools.getFlexibleXML();
                                                                                    java.util.List<android.graphics.Rect> scaleElements = new ArrayList<>();

                                                                                    int preciseAnsY = finalTargetY + 100; // Khởi tạo áng chừng
                                                                                    try {
                                                                                        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                        org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                        org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                        for (int offset = 0; offset <= 1000; offset += 50) {
                                                                                            int testY = finalTargetY + offset;
                                                                                            for (int i = 0; i < nodes.getLength(); i++) {
                                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                                android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                                if (r != null && Math.abs(r.centerY() - testY) <= 40 && r.width() > 20 && r.width() < 300) {
                                                                                                    scaleElements.add(r);
                                                                                                }
                                                                                            }
                                                                                            if (scaleElements.size() >= 2) {
                                                                                                preciseAnsY = testY;
                                                                                                break;
                                                                                            } else {
                                                                                                scaleElements.clear();
                                                                                            }
                                                                                        }

                                                                                        // Gọt trùng lặp (Deduplicate) y như Matrix
                                                                                        if (!scaleElements.isEmpty()) {
                                                                                            List<android.graphics.Rect> uniqueBoxes = new ArrayList<>();
                                                                                            for (android.graphics.Rect r : scaleElements) {
                                                                                                boolean isDup = false;
                                                                                                for (android.graphics.Rect u : uniqueBoxes) {
                                                                                                    if (Math.abs(r.centerX() - u.centerX()) < 30) { isDup = true; break; }
                                                                                                }
                                                                                                if (!isDup) uniqueBoxes.add(r);
                                                                                            }
                                                                                            uniqueBoxes.sort(Comparator.comparingInt(r -> r.centerX()));

                                                                                            int targetIndex = Integer.parseInt(answerStr) - 1;
                                                                                            if (targetIndex >= 0 && targetIndex < uniqueBoxes.size()) {
                                                                                                finalClickPt = new android.graphics.Point(uniqueBoxes.get(targetIndex).centerX(), preciseAnsY);
                                                                                            }
                                                                                        }
                                                                                    } catch (Exception ignored) {}
                                                                                }
                                                                            }
                                                                            else
                                                                            {
                                                                                // CHIẾN THUẬT CHỮ (ĐÃ BỌC THÉP CHỐNG CẮN NGƯỢC)
                                                                                String cleanAns = normalizeText(answerStr).replaceAll("[^a-z0-9]", "");

                                                                                HSQTools.TextBlock ansNode = currentVisible.stream()
                                                                                        .filter(x ->
                                                                                        {
                                                                                            if (finalTargetY != -1) {
                                                                                                // LUẬT TỬ HÌNH: Nằm dưới Header
                                                                                                return x.y > finalTargetY + 20 && x.y < finalTargetY + 1500;
                                                                                            }
                                                                                            return x.y > 180;
                                                                                        })
                                                                                        .filter(x ->
                                                                                        {
                                                                                            String nodeTxt = normalizeText(x.text).replaceAll("[^a-z0-9]", "");
                                                                                            boolean isTextMatch = nodeTxt.equals(cleanAns) ||
                                                                                                    (nodeTxt.contains(cleanAns) && nodeTxt.length() <= cleanAns.length() + 5) ||
                                                                                                    HSQTools.levenshtein(nodeTxt, cleanAns) <= (cleanAns.length() * 0.2);
                                                                                            return isTextMatch;
                                                                                        })
                                                                                        // TÌM CHỮ THÌ ƯU TIÊN THẰNG CÓ Y NHỎ NHẤT (Sát dưới Header nhất)
                                                                                        .min(Comparator.comparingInt(x -> x.y))
                                                                                        .orElse(null);

                                                                                if (ansNode != null) {
                                                                                    int finalClickX = ansNode.x;
                                                                                    int finalClickY = ansNode.y;

                                                                                    if (finalClickX > 400 && finalClickX < 1200) {
                                                                                        String xml = HSQTools.getFlexibleXML();
                                                                                        try {
                                                                                            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                            for (int i = 0; i < nodes.getLength(); i++) {
                                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                                String rawText = node.getAttribute("text") + " " + node.getAttribute("content-desc");
                                                                                                String xmlText = normalizeText(rawText).replaceAll("[^a-z0-9]", "");

                                                                                                if (!xmlText.isEmpty() && (xmlText.equals(cleanAns) || xmlText.contains(cleanAns))) {
                                                                                                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                                    if (r != null && Math.abs(r.centerY() - finalClickY) <= 40) {
                                                                                                        if (r.width() < (widthOfScreen * 0.5) && r.centerX() > 0) {
                                                                                                            finalClickX = r.centerX();
                                                                                                        } else if (r.width() >= (widthOfScreen * 0.5)) {
                                                                                                            finalClickX = r.left + 80;
                                                                                                        }
                                                                                                        break;
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        } catch (Exception ignored) {}
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
                                                                                if (targetY != -1 && targetY > 350)
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
                                                                                swipe(xs, ysTop, xs, ysBot, 1500);
                                                                            }

                                                                            delay(2500);
                                                                            continue checkBlockActionLoop;
                                                                        }

                                                                        // =======================================================
                                                                        // 5. THỰC THI CLICK (NẾU TÌM THẤY)
                                                                        // =======================================================
                                                                        if (finalClickPt != null)
                                                                        {
                                                                            click(finalClickPt.x, finalClickPt.y, false);
                                                                            delay(2000);
                                                                            break checkBlockActionLoop;
                                                                        }
                                                                    }
                                                                }
                                                                break checkBlockSmartLoop;
                                                            }
                                                        }
                                                        else if (step.contains("auto_choose"))
                                                        {
                                                            Matcher match = java.util.regex.Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                            if (match.find() && match.group(1).contains("~"))
                                                            {
                                                                String[] parts = match.group(1).split("~");
                                                                String headerStr = parts[0];
                                                                String answerValue = parts[1];

                                                                updateNotificationContent("🤖 Auto Choose: Cố gắng chọn [" + answerValue + "]");
                                                                String normAnswer = HSQTools.getOnlyTextLinq(normalizeText(answerValue));

                                                                // =======================================================
                                                                // 🎯 GIAI ĐOẠN 1: TÌM TRỰC TIẾP TRÊN MÀN HÌNH (Dành cho Radio/Checkbox hiển thị sẵn)
                                                                // =======================================================
                                                                List<TextBlock> currentVisible = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                                TextBlock directTarget = currentVisible.stream()
                                                                        .filter(x ->
                                                                        {
                                                                            String txt = HSQTools.getOnlyTextLinq(normalizeText(x.text));
                                                                            return txt.equals(normAnswer) || txt.contains(normAnswer) || HSQTools.levenshtein(txt, normAnswer) <= (normAnswer.length() * 0.2);
                                                                        })
                                                                        .min(Comparator.comparingInt(x -> Math.abs(x.x - xCenter))) // Ưu tiên gần tâm
                                                                        .orElse(null);

                                                                if (directTarget != null)
                                                                {
                                                                    updateNotificationContent("Thấy ngay đáp án trên màn hình! Click: " + directTarget.text);
                                                                    click(directTarget.x, directTarget.y, false);
                                                                    delay(2000);
                                                                    previousText = answerValue;
                                                                }
                                                                else
                                                                {
                                                                    // =======================================================
                                                                    // 🎯 GIAI ĐOẠN 2: KHÔNG THẤY ĐÁP ÁN -> TRUY TÌM HEADER ĐỂ MỞ DROPDOWN/INPUT
                                                                    // =======================================================
                                                                    updateNotificationContent("Không thấy đáp án. Đang tìm Câu hỏi để mở Dropdown...");
                                                                    String normHeader = HSQTools.getOnlyTextLinq(normalizeText(headerStr));
                                                                    TextBlock headerTarget = currentVisible.stream()
                                                                            .filter(x ->
                                                                            {
                                                                                String txt = HSQTools.getOnlyTextLinq(normalizeText(x.text));
                                                                                return txt.equals(normHeader) || txt.contains(normHeader) || normHeader.contains(txt) || HSQTools.levenshtein(txt, normHeader) <= (normHeader.length() * 0.15);
                                                                            })
                                                                            .min(Comparator.comparingInt(x -> x.y)) // Ưu tiên cao nhất
                                                                            .orElse(null);

                                                                    if (headerTarget != null)
                                                                    {
                                                                        // Thấy Header rồi! Ta sẽ chọt vào tọa độ CỦA THẰNG CON NẰM NGAY DƯỚI NÓ
                                                                        // Bằng cách dùng ASBL hoặc tính toán tọa độ
                                                                        AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
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
                                                                            inputText(answerValue, null, false);
                                                                            delay(2000);
                                                                            previousText = answerValue;
                                                                        }
                                                                        else
                                                                        {
                                                                            // Bàn phím không lên -> List Dropdown vừa được xổ ra
                                                                            updateNotificationContent("Đã xổ Dropdown -> Tìm đáp án lần 2");
                                                                            List<TextBlock> afterOpenVisible = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());

                                                                            TextBlock dropdownTarget = afterOpenVisible.stream()
                                                                                    .filter(x ->
                                                                                    {
                                                                                        String txt = HSQTools.getOnlyTextLinq(normalizeText(x.text));
                                                                                        return txt.equals(normAnswer) || txt.contains(normAnswer) || HSQTools.levenshtein(txt, normAnswer) <= (normAnswer.length() * 0.2);
                                                                                    })
                                                                                    .min(Comparator.comparingInt(x -> Math.abs(x.x - xCenter)))
                                                                                    .orElse(null);

                                                                            if (dropdownTarget != null)
                                                                            {
                                                                                click(dropdownTarget.x, dropdownTarget.y, false);
                                                                                delay(2000);
                                                                                previousText = answerValue;
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
                                                                    screenBegin = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
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
                                                            List<HSQTools.TextBlock> checkUserAct1 = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                            while (true)
                                                            {
                                                                updateNotificationContent("Lỗi: " + step);
                                                                List<HSQTools.TextBlock> checkUserAct2 = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                                if (!HSQTools.areAlmostSame(checkUserAct1, checkUserAct2, 20))
                                                                {
                                                                    break;
                                                                }
                                                                delay(180000);
                                                            }
                                                            AINguL = 0;
                                                            createAgain = 0;
                                                            currentState = STATE_GET_ANSWER;
                                                            continue stateMachine;
                                                        }
                                                        createAgain++;
                                                        createNewChatGemByApi(customAgentRule, true);
                                                        currentState = STATE_GET_ANSWER;
                                                    }
                                                    else
                                                    {
                                                        AINguL++;
                                                        screenBegin = getScreenText().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                                                        textAnswer = sendChatToGemByApi("lỗi phân tích cú pháp, kiểm tra lại các rule và trả lời lại câu trên");
                                                        currentState = STATE_ANSWER_OK;
                                                    }
                                                    continue stateMachine;
                                                }

                                                clickChoose = 0;
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
                            continue buserLoop;
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

    public static boolean isConnectedToInternet(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null)
        {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
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
            }
            catch (Exception ignored)
            {
            }
            JSONObject controlserver = HSQHttps.postRequest("http://quaykute.zapto.org:3000/api/user/control", bodyPost.toString(), JSONObject.class, false);
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
                        apiZoneToken = control.getString("apiZoneToken");
                        aiModel = control.getString("aiModel");
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

    private boolean upDateTool()
    {
        if (VCode < apkVersion)
        {
            show();
            delay(2000);
            updateContent("down apk " + apkVersion);
            beginInstall:
            while (true)
            {
                int tryReinstall = 1;
                String linkDownLoad = "http://quaykute.zapto.org:3000/apk/SHTools" + apkVersion + ".apk";
                String filePath = "/sdcard/Download/SHTools.apk";
                File fileDeLuu = new File(filePath);

                if (HSQHttps.downloadFile(linkDownLoad, fileDeLuu))
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
                                    updateContent("Lỗi cài apk " + tryReinstall);
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
                            while(true) {
                                int checkInstall = ASBLBridgeService.findMultiTextDesWindow(360, true, true, true, false, "decline", "done");
                                if(checkInstall == 1) {
                                    delay(5000);
                                }
                                else {
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
                        linkDownLoad = "http://" + localServerIp + ":5000/download/apk/SHTools" + apkVersion + ".apk";
                    }
                    else
                    {
                        linkDownLoad = "http://quay.hopto.org:5000/download/apk/SHTools" + apkVersion + ".apk";
                    }
                    if (HSQHttps.downloadFile(linkDownLoad, fileDeLuu))
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
                                        updateContent("Lỗi cài apk " + tryReinstall);
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

                                while(true) {
                                    int checkInstall = ASBLBridgeService.findMultiTextDesWindow(360, true, true, true, false, "decline", "done");
                                    if(checkInstall == 1) {
                                        delay(5000);
                                    }
                                    else {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        updateContent("down apk dp" + apkVersion);
                        String linkDownLoadDP = "http://quaykute.id.vn/apk/SHTools" + apkVersion + ".apk";
                        if (HSQHttps.downloadFile(linkDownLoadDP, fileDeLuu))
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
                                            updateContent("Lỗi cài apk " + tryReinstall);
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

                                    while(true) {
                                        int checkInstall = ASBLBridgeService.findMultiTextDesWindow(360, true, true, true, false, "decline", "done");
                                        if(checkInstall == 1) {
                                            delay(5000);
                                        }
                                        else {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            updateContent("không thể download");
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

    private boolean updatePromt()
    {
        show();
        delay(2000);
        // 1. Lấy version prompt hiện tại đang lưu trong máy (mặc định chưa có là 0)
        android.content.SharedPreferences prefs = com.quayquay.hsq.tools.HSQConfig.getContext()
                .getSharedPreferences("QQ_PREFS_DATA", android.content.Context.MODE_PRIVATE);
        int localPromtVersion = prefs.getInt("PROMT_VERSION", 0);

        // 2. Nếu máy đang chạy bản cũ hơn bản trên server -> Tiến hành lôi về
        if (localPromtVersion < remotePromtVersion)
        {
            updateContent("down promt v" + remotePromtVersion);

            // Đảm bảo thư mục lưu trữ luôn tồn tại
            HSQFileHelper.createFolder("/sdcard/Servey");
            String filePath = "/sdcard/Servey/PromtGem.txt";
            File fileDeLuu = new File(filePath);

            while (true)
            {
                String linkDownLoad = "http://quaykute.zapto.org:3000/apk/PromtGem.txt";
                if (HSQHttps.downloadFile(linkDownLoad, fileDeLuu, false))
                {
                    if (fileDeLuu.exists() && fileDeLuu.length() > 500)
                    { // Đảm bảo file tải về chứa chữ thật (>500 bytes)
                        // Tải thành công -> Khóa cứng mốc version mới vào SharedPreferences
                        prefs.edit().putInt("PROMT_VERSION", remotePromtVersion).apply();
                        updateContent("Đã update Promt v" + remotePromtVersion);
                        delay(2000);
                        hide();
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
                            updateContent("Đã update Promt v" + remotePromtVersion);
                            delay(2000);
                            hide();
                            return true;
                        }
                    }
                    else
                    {
                        updateContent("down promt dp v" + remotePromtVersion);
                        // LINK DỰ PHÒNG CHÍNH THỨC NGOÀI INTERNET
                        String linkDownLoadDP = "http://quaykute.id.vn/apk/PromtGem.txt";
                        if (HSQHttps.downloadFile(linkDownLoadDP, fileDeLuu, false))
                        {
                            if (fileDeLuu.exists() && fileDeLuu.length() > 500)
                            {
                                prefs.edit().putInt("PROMT_VERSION", remotePromtVersion).apply();
                                updateContent("Đã update Promt v" + remotePromtVersion);
                                delay(2000);
                                hide();
                                return true;
                            }
                        }
                        else
                        {
                            updateContent("Lỗi tải Promt từ xa!");
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

    private boolean updateProfile()
    {
        show();
        delay(2000);
        updateContent("down promt v" + remotePromtVersion);

        // Đảm bảo thư mục lưu trữ luôn tồn tại
        HSQFileHelper.createFolder("/sdcard/Servey");
        File fileDeLuu = new File(pathInfoProfileSaved);

        while (true)
        {
            String linkDownLoad = "http://quaykute.zapto.org:3000/servey_profile/sv_" + deviceID + ".json";
            if (HSQHttps.downloadFile(linkDownLoad, fileDeLuu, false))
            {
                if (fileDeLuu.exists() && fileDeLuu.length() > 500)
                {
                    delay(2000);
                    hide();
                    return true;
                }
            }
            else
            {
                updateContent("Lỗi tải profile từ xa!");
                HSQTools.delay(10000);
            }
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

    private String getCloneFromServer(String api_key, String Display, int type, boolean isCheckLive, int timeGet)
    {
        String clone;
        int i = 0;
        while (true)
        {
            i++;
            updateNotificationContent("get " + Display + " " + i);
            JSONObject bodyPost = new JSONObject();
            try
            {
                bodyPost.put("api_key", api_key);
                bodyPost.put("category_id", 4);
            }
            catch (Exception ignored)
            {
            }
            JSONObject body = HSQHttps.postRequest("http://quaykute.zapto.org:3000/api/clones/get-live", bodyPost.toString(), JSONObject.class, false);
            if (body != null)
            {
                try
                {
                    String isSuccess = body.getString("status");
                    if (isSuccess.contains("success"))
                    {
                        try
                        {
                            String clonetest = (body.getJSONObject("data")).getString("noi_dung");
                            if (isCheckLive)
                            {
                                if (clonetest.contains("|"))
                                {
                                    try
                                    {
                                        if (HSQFacebook.isLiveUid(clonetest.split(Pattern.quote("|"))[0]))
                                        {
                                            clone = clonetest;
                                            break;
                                        }
                                    }
                                    catch (Exception ignored)
                                    {
                                    }
                                }
                                else
                                {
                                    if (HSQFacebook.isLiveUid(clonetest))
                                    {
                                        clone = clonetest;
                                        break;
                                    }
                                }
                            }
                            else
                            {
                                clone = clonetest;
                                break;
                            }
                        }
                        catch (Exception ignored)
                        {
                        }
                    }
                    else if (isSuccess.contains("empty"))
                    {
                        show();
                        updateContent("Hết " + Display);
                        delay(60000);
                        hide();
                        delay(2000);
                    }
                }
                catch (Exception ignored)
                {
                }
            }
            if (timeGet != 999 && i >= timeGet)
            {
                clone = "|noclone|";
                break;
            }
            HSQTools.delay(10000);
        }
        return clone;
    }

    private String getNewApiGeminiKey()
    {
        while (true)
        {
            try
            {
                String getGemKey = getCloneFromServer(apiAdminServer, "Key Gem", 3, false, 999);
                if (getGemKey.contains("|"))
                {
                    String[] splgetGemKey = getGemKey.split(Pattern.quote("|"));
                    return splgetGemKey[1];
                }
            }
            catch (Exception ignored)
            {
                delay(5000);
            }
        }
    }

    @Override
    public boolean onPauseServiceByVolume()
    {
        if (geminiAI != null)
        {
            geminiAI.saveHistory();
            geminiAI.freeRam();
        }
        return true;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        isStop = true;
    }

    // Khai báo Regex tĩnh 1 lần duy nhất để không bị build lại regex trong vòng lặp
    private static final java.util.regex.Pattern DIACRITICS_PATTERN = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public String removeAccents(String input)
    {
        if (input == null) return "";
        // Dùng Normalizer chuẩn của Java, tốc độ nhanh gấp 100 lần Transliterator
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
    }

    private String normalizeText(String input)
    {
        if (input == null) return "";
        return removeAccents(input).replaceAll("\\s+", "").toLowerCase();
    }

    private List<TextBlock> normalizeAndOrderRewards(List<TextBlock> rawList)
    {
        List<TextBlock> finalResult = new ArrayList<>();
        if (rawList == null || rawList.isEmpty()) return finalResult;

        Pattern pointPattern = Pattern.compile("([\\d,]+)\\s*(points|pts)", Pattern.CASE_INSENSITIVE);
        Pattern minutePattern = Pattern.compile("(\\d+\\+?)\\s*(minutes|mins|min)", Pattern.CASE_INSENSITIVE);
        Pattern starPattern = Pattern.compile("([\\d.]+)\\s*/\\s*5", Pattern.CASE_INSENSITIVE);

        List<List<TextBlock>> screens = new ArrayList<>();
        List<TextBlock> currentScreen = new ArrayList<>();
        int lastY = -1;

        // 1. TÁCH RIÊNG TỪNG LẦN VUỐT (Chống ăn cắp thẻ của màn hình khác)
        for (TextBlock block : rawList)
        {
            if (block.text == null || block.text.trim().isEmpty()) continue;

            // Nếu Y tự nhiên rớt xuống một mảng lớn (> 800px) -> Chắc chắn là do vừa swipe sang màn mới
            if (lastY != -1 && block.y < lastY - 800)
            {
                if (!currentScreen.isEmpty())
                {
                    screens.add(currentScreen);
                    currentScreen = new ArrayList<>();
                }
            }
            currentScreen.add(block);
            lastY = block.y;
        }
        if (!currentScreen.isEmpty())
        {
            screens.add(currentScreen);
        }

        List<String> recentSignatures = new ArrayList<>();

        // 2. XỬ LÝ GHÉP CẶP TRONG NỘI BỘ TỪNG MÀN HÌNH
        for (List<TextBlock> screenBlocks : screens)
        {
            List<TextBlock> pts = new ArrayList<>();
            List<TextBlock> mins = new ArrayList<>();
            List<TextBlock> stars = new ArrayList<>();

            for (TextBlock b : screenBlocks)
            {
                if (pointPattern.matcher(b.text).find()) pts.add(b);
                else if (minutePattern.matcher(b.text).find()) mins.add(b);
                else if (starPattern.matcher(b.text).find()) stars.add(b);
            }

            for (TextBlock p : pts)
            {
                TextBlock closestMin = null;
                TextBlock closestStar = null;

                int minDistanceY = 800;
                int starDistanceY = 600;

                for (TextBlock m : mins)
                {
                    if (Math.abs(m.x - p.x) < 400)
                    {
                        int distY = Math.abs(p.y - m.y);
                        if (distY < minDistanceY)
                        {
                            minDistanceY = distY;
                            closestMin = m;
                        }
                    }
                }

                for (TextBlock s : stars)
                {
                    if (Math.abs(s.x - p.x) < 400)
                    {
                        int distY = Math.abs(s.y - p.y);
                        if (distY < starDistanceY)
                        {
                            starDistanceY = distY;
                            closestStar = s;
                        }
                    }
                }

                String minText = (closestMin != null) ? closestMin.text : "NO_MIN";
                String starText = (closestStar != null) ? closestStar.text : "NO_STAR";
                String signature = p.text + "|" + minText + "|" + starText;

                // Chống trùng thẻ khi vuốt bị dính lại màn cũ
                if (recentSignatures.contains(signature)) continue;

                recentSignatures.add(signature);
                if (recentSignatures.size() > 15) recentSignatures.remove(0);

                finalResult.add(p);
                if (closestMin != null)
                {
                    finalResult.add(closestMin);
                    mins.remove(closestMin);
                }
                if (closestStar != null)
                {
                    finalResult.add(closestStar);
                    stars.remove(closestStar);
                }
            }
        }

        return finalResult;
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
                + dashes.toString();

        // 5. Ghi nối vào file bằng hàm siêu tốc của bạn
        HSQFileHelper.writetoFile(logContent, logFilePath);
    }

    // =========================================================
    // 1. TẠO MỚI HOẶC LOAD CHAT
    // =========================================================
    private void createNewChatGemByApi(String customAgentRule, boolean deleteOldChat)
    {
        updateNotificationContent("Đang thiết lập Context API...");

        if (deleteOldChat)
        {
            geminiAI.deleteChat(); // Xóa sạch lịch sử
        }

        geminiAI.loadOrCreateChat(customAgentRule);
        HSQTools.delay(1000);
    }

    // =========================================================
    // 2. TỰ ĐỘNG CHỤP ẢNH VÀ GỬI LÊN GEMINI
    // =========================================================
    private String getAnswerFromGemByApi(int imageCount, boolean splitAnswer, boolean captureScreen, String prompt)
    {
        int tryAgain = 0;
        String textAnswer = "";
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
                    swipe(xCenter, yBot, xCenter, yTop, 1500);
                    HSQTools.delay(2000);

                    String fileName = (i + 1 < 10) ? "/screenCapa" + (i + 1) + ".png" : "/screenCapb" + (i - 8) + ".png";
                    HSQTools.cropAndSaveScreen(fullScreen, imagePath + fileName);
                    HSQTools.delay(1000);
                }
                for (int i = 0; i < imageCount; i++)
                {
                    swipe(xCenter, yTop, xCenter, yBot, 1500);
                    HSQTools.delay(2000);
                }
            }
        }
        HSQTools.ScanImage(imagePath);
        HSQTools.delay(2000);
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
                    files, new java.util.Comparator<File>()
                    {
                        @Override
                        public int compare(File f1, File f2)
                        {
                            return f1.getName().compareTo(f2.getName());
                        }
                    }
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
        while (true)
        {
            try
            {
                Log.d("TEST_TREO", "1. Chuẩn bị nhảy vào hàm sendMessageWithImages");
                textAnswer = geminiAI.sendMessageWithImages(prompt, listBase64Images);
                Log.d("TEST_TREO", "4. Đã thoát ra khỏi hàm, kết quả là: " + textAnswer);
                if (textAnswer.startsWith("API Error: 429") || textAnswer.contains("\"code\": 429") || textAnswer.contains("\"status\": \"RESOURCE_EXHAUSTED\"")
                        || textAnswer.contains("Quota exceeded for metric") || textAnswer.contains("\"code\": 403")
                        || textAnswer.contains("has been suspended"))
                {
//                    apiGemini = getNewApiGeminiKey();
//                    geminiAI.setApiKey(apiGemini);
                    //geminiAI.deleteChat(); // Dọn dẹp não cũ
                    //createNewChatGemByApi(customAgentRule, false);
                    delay(15000);
                    continue;
                }
                else if (textAnswer.startsWith("API Error:") || textAnswer.startsWith("Exception:"))
                {
                    updateNotificationContent("Lỗi API/Mạng: " + textAnswer + ". Đợi 15s...");
                    HSQTools.delay(15000);
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
                updateNotificationContent("API Trả về: " + textAnswer);
                break;
            }
            catch (Exception ex)
            {
                updateNotificationContent("Lỗi kết nối...");
                HSQTools.delay(15000);
            }
        }

        geminiAI.saveTurnToHistory(prompt, listBase64Images, textAnswer);
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
        updateNotificationContent("Gửi chat đến API: " + chatContent);

        while (true)
        {
            try
            {
                updateNotificationContent("Đang chờ API trả lời...");
                String textAnswer = geminiAI.sendMessageWithImages(chatContent, null);
                updateNotificationContent("API Trả về: " + textAnswer);

                if (textAnswer.startsWith("API Error: 429") || textAnswer.contains("\"code\": 429") ||
                        textAnswer.contains("\"status\": \"RESOURCE_EXHAUSTED\"") || textAnswer.contains("Quota exceeded for metric") || textAnswer.contains("\"code\": 403")
                        || textAnswer.contains("has been suspended"))
                {
//                    apiGemini = getNewApiGeminiKey();
//                    geminiAI.setApiKey(apiGemini);
//                    geminiAI.deleteChat(); // Dọn dẹp não cũ
//                    createNewChatGemByApi(customAgentRule, false);
                    delay(15000);
                    continue;
                }
                else if (textAnswer.startsWith("API Error:") || textAnswer.startsWith("Exception:"))
                {
                    updateNotificationContent("Lỗi API/Mạng: " + textAnswer + ". Đợi 15s...");
                    HSQTools.delay(15000);
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
                return textAnswer;
            }
            catch (Exception ex)
            {
                updateNotificationContent("Lỗi kết nối...");
                HSQTools.delay(15000);
            }
        }
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

    // Thêm hàm helper này ở ngoài để quét chữ cho lẹ (giống C#)
    private List<HSQTools.TextBlock> getScreenText()
    {
        AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
        List<HSQTools.TextBlock> list = HSQTools.readAllTextOnScreenByASBL(root);
        if (root != null) root.recycle(); // Chống tràn RAM
        return list;
    }


    private void clearAllText()
    {
        inputText("", null, false);
    }

    /**
     * Lấy XML linh hoạt kết hợp siêu tốc độ của ASBL và sự trâu bò của Uiautomator
     */
    private String getFlexibleXML()
    {
        // 1. Lấy XML thẳng từ não của ASBL (Siêu tốc độ 10ms - Không cần Socket)
        String xml = getXmlFromASBL();

        // 2. Kiểm tra xem XML có bị "MÙ" (tàng hình WebView) không
        // Cắt chuỗi theo chữ "<node" để đếm số lượng tag, nhanh hơn dùng Regex
        int nodeCount = 0;
        if (xml != null && !xml.isEmpty())
        {
            nodeCount = xml.split("<node").length - 1;
        }

        // 3. Nếu XML rỗng hoặc số lượng Node <= 5 (Bẫy tàng hình) -> QUAY XE!
        if (xml == null || xml.isEmpty() || nodeCount <= 5)
        {
            android.util.Log.d("HSQTools", "ASBL bị mù/rỗng -> Lấy qua Uiautomator Dump...");

            // 4. Lôi hàng cổ uiautomator dump ra xài (Nhờ Root đấm)
            // Không cần bật tắt app qqextension nữa vì ta đéo xài nó nữa!
            xml = getXmlFromUiautomator();
        }

        return xml;
    }

    // =======================================================
    // VŨ KHÍ 1: TỰ BUILD XML TỪ QUYỀN TRỢ NĂNG (NHANH NHƯ CHỚP)
    // =======================================================
    private String getXmlFromASBL()
    {
        try
        {
            AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
            if (root == null) return "";

            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n");
            sb.append("<hierarchy rotation=\"0\">\n");

            // Gọi hàm đệ quy để quét sạch Node và vẽ ra XML
            dumpNodeRec(root, sb, 0);

            sb.append("</hierarchy>");
            root.recycle(); // Giải phóng
            return sb.toString();
        }
        catch (Exception e)
        {
            android.util.Log.e("HSQTools", "Lỗi build XML từ ASBL: " + e.getMessage());
            return "";
        }
    }

    // Đệ quy vẽ từng thẻ <node> y hệt định dạng của Uiautomator để Regex của sếp bắt đéo trượt phát nào
    private void dumpNodeRec(android.view.accessibility.AccessibilityNodeInfo node, StringBuilder sb, int index)
    {
        if (node == null) return;

        sb.append("<node index=\"").append(index).append("\" ");
        sb.append("text=\"").append(escapeXml(node.getText())).append("\" ");
        sb.append("resource-id=\"").append(escapeXml(node.getViewIdResourceName())).append("\" ");
        sb.append("class=\"").append(escapeXml(node.getClassName())).append("\" ");
        sb.append("package=\"").append(escapeXml(node.getPackageName())).append("\" ");
        sb.append("content-desc=\"").append(escapeXml(node.getContentDescription())).append("\" ");

        // 🌟 KHẢM THÊM THẰNG NÀY ĐỂ XML FALLBACK CÓ DỮ LIỆU ĐỂ LỌC
        sb.append("visible-to-user=\"").append(node.isVisibleToUser()).append("\" ");

        sb.append("checkable=\"").append(node.isCheckable()).append("\" ");
        sb.append("checked=\"").append(node.isChecked()).append("\" ");
        sb.append("clickable=\"").append(node.isClickable()).append("\" ");
        sb.append("enabled=\"").append(node.isEnabled()).append("\" ");
        sb.append("focusable=\"").append(node.isFocusable()).append("\" ");
        sb.append("focused=\"").append(node.isFocused()).append("\" ");
        sb.append("scrollable=\"").append(node.isScrollable()).append("\" ");
        sb.append("long-clickable=\"").append(node.isLongClickable()).append("\" ");
        sb.append("password=\"").append(node.isPassword()).append("\" ");
        sb.append("selected=\"").append(node.isSelected()).append("\" ");

        android.graphics.Rect bounds = new android.graphics.Rect();
        node.getBoundsInScreen(bounds);
        sb.append("bounds=\"[").append(bounds.left).append(",").append(bounds.top).append("][")
                .append(bounds.right).append(",").append(bounds.bottom).append("]\" ");

        int childCount = node.getChildCount();
        if (childCount == 0)
        {
            sb.append("/>\n");
        }
        else
        {
            sb.append(">\n");
            for (int i = 0; i < childCount; i++)
            {
                android.view.accessibility.AccessibilityNodeInfo child = node.getChild(i);
                if (child != null)
                {
                    dumpNodeRec(child, sb, i);
                    child.recycle();
                }
            }
            sb.append("</node>\n");
        }
    }

    // Hàm chống lỗi XML (Kẻo Regex parse bị ngáo)
    private String escapeXml(CharSequence text)
    {
        if (text == null) return "";
        return text.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // =======================================================
    // VŨ KHÍ 2: LẤY XML CHẬM NHƯNG TRÂU BÒ BẰNG ROOT SHELL
    // =======================================================
    private String getXmlFromUiautomator()
    {
        if (HSQTools.isRooted())
        {
            String path = "/sdcard/window_dump.xml";
            // Xóa file cũ
            HSQRoot.execute("rm -f " + path);
            // Ép hệ thống dump UI ra file
            HSQRoot.execute("uiautomator dump " + path);
            delay(500); // Chờ nó dump xong

            // Đọc file lên
            return HSQFileHelper.readTextFile(path);
        }
        return "";
    }

    /**
     * Mắt thần ASBL: Chuyên săn các nút Next, Continue, Accept ở nửa dưới màn hình
     */
    private android.graphics.Point findNextButtonByASBL(android.view.accessibility.AccessibilityNodeInfo root)
    {
        if (root == null) return null;

        java.util.List<android.view.accessibility.AccessibilityNodeInfo> allNodes = new java.util.ArrayList<>();
        getAllNodesRec(root, allNodes);

        // ĐÃ BỔ SUNG THÊM TỪ KHÓA: accept, take the survey, start, agree...
        String regex = ".*(continue|next|submit|tieptuc|tieptheo|done|gui|send|batdau|accept|take the survey|start|agree|đồng ý).*";

        android.view.accessibility.AccessibilityNodeInfo bestNode = null;
        int maxY = -1; // Ưu tiên thằng nào nằm dưới cùng màn hình nhất

        for (android.view.accessibility.AccessibilityNodeInfo node : allNodes)
        {
            CharSequence text = node.getText();
            CharSequence desc = node.getContentDescription();
            String nodeContent = "";

            if (text != null)
                nodeContent += text.toString().toLowerCase().replaceAll("[^a-z0-9 ]", "");
            if (desc != null)
                nodeContent += desc.toString().toLowerCase().replaceAll("[^a-z0-9 ]", "");

            if (nodeContent.matches(regex))
            {
                android.graphics.Rect bounds = new android.graphics.Rect();
                node.getBoundsInScreen(bounds);

                // Nút Next thường nằm ở nửa dưới màn hình (Y > 1000) và diện tích phải đàng hoàng
                if (bounds.centerY() > maxY && bounds.centerY() > 1000 && bounds.width() > 50)
                {
                    maxY = bounds.centerY();
                    bestNode = node;
                }
            }
        }

        if (bestNode != null)
        {
            android.graphics.Rect bounds = new android.graphics.Rect();
            bestNode.getBoundsInScreen(bounds);
            return new android.graphics.Point(bounds.centerX(), bounds.centerY());
        }

        return null;
    }

    // Hàm đệ quy phụ trợ để gom tất cả node trên màn hình
    private void getAllNodesRec(android.view.accessibility.AccessibilityNodeInfo node, java.util.List<android.view.accessibility.AccessibilityNodeInfo> list)
    {
        if (node == null) return;
        list.add(node);
        for (int i = 0; i < node.getChildCount(); i++)
        {
            getAllNodesRec(node.getChild(i), list);
        }
    }

    /**
     * Mắt thần dò ô Input: Tìm ô cho phép nhập liệu nằm gần dòng Text nhất
     */
    private android.graphics.Rect findInputNearYByASBL(android.view.accessibility.AccessibilityNodeInfo root, int labelY)
    {
        if (root == null) return null;
        java.util.List<android.view.accessibility.AccessibilityNodeInfo> allNodes = new java.util.ArrayList<>();
        getAllNodesRec(root, allNodes); // Tận dụng lại hàm getAllNodesRec đã viết ở bước trước

        for (android.view.accessibility.AccessibilityNodeInfo node : allNodes)
        {
            // Khắc tinh của ô nhập liệu: isEditable()
            if (node.isEditable() || (node.getClassName() != null && node.getClassName().toString().contains("EditText")))
            {
                android.graphics.Rect bounds = new android.graphics.Rect();
                node.getBoundsInScreen(bounds);

                // Nằm gần cái label chứa câu hỏi (Lệch Y tối đa 250 pixel)
                if (Math.abs(bounds.centerY() - labelY) < 250 && bounds.width() > 50)
                {
                    return bounds;
                }
            }
        }
        return null;
    }

    /**
     * Mắt thần bắt Nút: Bắn tọa độ Text vào, dò lên xem thằng cha nào Click được thì lấy tọa độ thằng Cha
     */
    private android.graphics.Point findClickableParentByASBL(android.view.accessibility.AccessibilityNodeInfo root, int targetX, int targetY)
    {
        if (root == null) return null;
        java.util.List<android.view.accessibility.AccessibilityNodeInfo> allNodes = new java.util.ArrayList<>();
        getAllNodesRec(root, allNodes);

        for (android.view.accessibility.AccessibilityNodeInfo node : allNodes)
        {
            android.graphics.Rect bounds = new android.graphics.Rect();
            node.getBoundsInScreen(bounds);

            // Tìm trúng cái Node chứa tọa độ Text
            if (bounds.contains(targetX, targetY))
            {
                // Check ngược lên các đời cha (tối đa 3 đời) xem có ông nào click được không
                android.view.accessibility.AccessibilityNodeInfo parent = node;
                int depth = 0;
                while (parent != null && depth < 3)
                {
                    if (parent.isClickable() || parent.isCheckable())
                    {
                        android.graphics.Rect parentBounds = new android.graphics.Rect();
                        parent.getBoundsInScreen(parentBounds);
                        return new android.graphics.Point(parentBounds.centerX(), parentBounds.centerY());
                    }
                    parent = parent.getParent();
                    depth++;
                }
            }
        }
        return null;
    }

    /**
     * Tương thích ngược cho luồng Matrix (không truyền chữ)
     */
    private android.graphics.Point findDropdownNearByASBL(android.view.accessibility.AccessibilityNodeInfo root, int labelX, int labelY)
    {
        return findDropdownNearByASBL(root, labelX, labelY, "");
    }

    /**
     * Mắt thần dò Dropdown (Bản nâng cấp tối thượng chống giật ngược):
     * Ép dò đúng hướng (Từ chữ trở xuống), và tự động thưởng điểm cho ô chứa đúng từ khóa đang tìm!
     */
    private android.graphics.Point findDropdownNearByASBL(android.view.accessibility.AccessibilityNodeInfo root, int labelX, int labelY, String targetContext)
    {
        if (root == null) return null;
        java.util.List<android.view.accessibility.AccessibilityNodeInfo> allNodes = new java.util.ArrayList<>();
        getAllNodesRec(root, allNodes);

        android.graphics.Point bestPoint = null;
        double minScore = Double.MAX_VALUE;

        for (android.view.accessibility.AccessibilityNodeInfo node : allNodes)
        {
            if (node.isClickable() || (node.getClassName() != null && node.getClassName().toString().contains("Spinner")))
            {
                android.graphics.Rect bounds = new android.graphics.Rect();
                node.getBoundsInScreen(bounds);

                int deltaX = Math.abs(bounds.centerX() - labelX);
                int deltaY = Math.abs(bounds.centerY() - labelY);

                // CHỐT CHẶN THÉP:
                // 1. Ô Dropdown bắt buộc phải nằm ngang hàng hoặc DƯỚI cái chữ (Lệch lên trên tối đa 30px thôi)
                // 2. Không được nằm quá xa phía dưới (Lệch Y < 350)
                if (bounds.centerY() >= labelY - 30 && deltaY < 350 && deltaX < 600 && bounds.width() > 30)
                {
                    double score = (deltaX * 1.5) + deltaY;

                    CharSequence txt = node.getText();
                    if (txt != null)
                    {
                        String nodeTxt = txt.toString().toLowerCase();
                        // Nếu cái hộp có chứa đúng cái chữ đang tìm (VD: Hộp "Năm" có chứa placeholder "Năm") -> Thưởng nóng!
                        if (targetContext != null && !targetContext.isEmpty() && nodeTxt.contains(targetContext))
                        {
                            score -= 500;
                        }
                    }

                    if (score < minScore)
                    {
                        minScore = score;
                        bestPoint = new android.graphics.Point(bounds.centerX(), bounds.centerY());
                    }
                }
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
        getAllNodesRec(root, allNodes);

        for (android.view.accessibility.AccessibilityNodeInfo node : allNodes)
        {
            String clazz = node.getClassName() != null ? node.getClassName().toString() : "";
            // Tóm cổ bọn có thuộc tính Checkable hoặc mang class Checkbox/Radio
            if (node.isCheckable() || clazz.contains("CheckBox") || clazz.contains("RadioButton"))
            {
                android.graphics.Rect r = new android.graphics.Rect();
                node.getBoundsInScreen(r);
                if (r.height() > 0 && r.width() > 0 && r.top < 2800 && r.top > minTop)
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
            Pattern pattern = Pattern.compile("<node\\s+([^>]*?)bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"");
            Matcher matcher = pattern.matcher(xml);
            while (matcher.find())
            {
                String attributes = matcher.group(1);
                int left = Integer.parseInt(matcher.group(2));
                int top = Integer.parseInt(matcher.group(3));
                int right = Integer.parseInt(matcher.group(4));
                int bottom = Integer.parseInt(matcher.group(5));

                int cx = (left + right) / 2;
                int cy = (top + bottom) / 2;

                if (cy > 180 && cy < (heightOfScreen - 150))
                {
                    String text = "";
                    Matcher textMatch = Pattern.compile("text=\"([^\"]*)\"").matcher(attributes);
                    if (textMatch.find()) text = textMatch.group(1);

                    String desc = "";
                    Matcher descMatch = Pattern.compile("content-desc=\"([^\"]*)\"").matcher(attributes);
                    if (descMatch.find()) desc = descMatch.group(1);

                    // Ưu tiên text, nếu trống trải thì vồ lấy content-desc
                    String finalTxt = (!text.trim().isEmpty()) ? text : desc;

                    if (!finalTxt.trim().isEmpty())
                    {
                        list.add(new HSQTools.TextBlock(finalTxt, cx, cy));
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
     * HÀM SIÊU CẤP v2.0: TRỊ BỆNH MÙ MỘT NỬA (HALF-BLIND WEBVIEW)
     */
    private List<HSQTools.TextBlock> getCheckAnswerSmart()
    {
        List<HSQTools.TextBlock> asblList = new ArrayList<>();
        List<HSQTools.TextBlock> ocrList = getOcrTextBlocks(); // Lấy OCR ngay từ đầu
        List<HSQTools.TextBlock> finalGrid = new ArrayList<>();

        // 🌟 SỔ BÌA ĐEN: Diệt rác hệ thống bằng Text cứng
        List<String> blacklist = Arrays.asList("back button", "offerwall");

        // =======================================================
        // BƯỚC 1: CÀO ASBL + DIỆT RÁC TEXT + ĂN CẮP TỌA ĐỘ 0
        // =======================================================
        AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
        if (root != null)
        {
            root.refresh();
            java.util.List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
            getAllNodesRec(root, allNodes);

            for (int i = 0; i < allNodes.size(); i++)
            {
                AccessibilityNodeInfo node = allNodes.get(i);
                CharSequence nodeText = node.getText();
                if (nodeText == null || nodeText.toString().trim().isEmpty())
                {
                    nodeText = node.getContentDescription();
                }

                if (nodeText != null && !nodeText.toString().trim().isEmpty())
                {
                    String rawText = nodeText.toString();

                    // 🔥 DIỆT RÁC TỪ VÒNG GỬI XE
                    if (blacklist.contains(rawText.toLowerCase().trim())) {
                        continue;
                    }

                    android.graphics.Rect bounds = new android.graphics.Rect();
                    node.getBoundsInScreen(bounds);

                    // Phân tích "Hồn ma bóng quế"
                    if (bounds.left <= 0 || bounds.width() > (widthOfScreen * 0.8)) {
                        for (int j = i + 1; j <= Math.min(i + 3, allNodes.size() - 1); j++) {
                            AccessibilityNodeInfo neighbor = allNodes.get(j);
                            if (neighbor != null) {
                                android.graphics.Rect neighborBounds = new android.graphics.Rect();
                                neighbor.getBoundsInScreen(neighborBounds);

                                if (neighborBounds.left > 10 && neighborBounds.width() < (widthOfScreen * 0.8) && neighborBounds.height() > 10) {
                                    bounds = neighborBounds;
                                    break;
                                }
                            }
                        }
                    }

                    int cx = bounds.centerX();
                    int cy = bounds.centerY();

                    // Sếp không muốn lọc Y = 350 nữa thì em trả về 180 như cũ cho sếp
                    if (cy > 180 && cy < (heightOfScreen - 150) && bounds.width() > 5 && bounds.height() > 5)
                    {
                        if (bounds.width() < (widthOfScreen * 0.9) && bounds.height() < 500) {
                            asblList.add(new HSQTools.TextBlock(rawText, cx, cy));
                        }
                    }
                }
            }
            root.recycle();
        }

        // =======================================================
        // BƯỚC 2: TĂNG CƯỜNG SỨC MẠNH "NUỐT CHỬNG" CỦA ASBL
        // =======================================================
        List<HSQTools.TextBlock> survivingOcrList = new ArrayList<>();

        for (HSQTools.TextBlock ocrNode : ocrList) {

            // 🔥 CŨNG DIỆT RÁC CHO THẰNG OCR LUÔN
            if (blacklist.contains(ocrNode.text.toLowerCase().trim())) {
                continue;
            }

            boolean isSwallowed = false;
            String cleanOcr = normalizeText(ocrNode.text).replaceAll("[^a-z0-9]", "");

            for (HSQTools.TextBlock asblNode : asblList) {
                String cleanAsbl = normalizeText(asblNode.text).replaceAll("[^a-z0-9]", "");

                // 🌟 MỞ RỘNG BÁN KÍNH NUỐT CHỬNG: Sai số Y 60px, sai số X 500px
                if (Math.abs(ocrNode.y - asblNode.y) <= 60 && Math.abs(ocrNode.x - asblNode.x) <= 500) {

                    // Gia tăng sai số Levenshtein cho câu dài (Sai 30% vẫn nuốt để giữ ASBL chuẩn)
                    int allowedError = Math.max(3, (int)(cleanAsbl.length() * 0.3));

                    if (cleanAsbl.contains(cleanOcr) || cleanOcr.contains(cleanAsbl) || HSQTools.levenshtein(cleanOcr, cleanAsbl) <= allowedError) {
                        isSwallowed = true;
                        break;
                    }
                }
            }

            if (!isSwallowed) {
                survivingOcrList.add(ocrNode);
            }
        }

        finalGrid.addAll(asblList);

        // =======================================================
        // BƯỚC 3: GOM CỤM OCR SÓT LẠI VÀ CHỐT SỔ
        // =======================================================
        if (!survivingOcrList.isEmpty()) {
            List<HSQTools.TextBlock> clusteredOcr = clusterTextBlocks(survivingOcrList);
            finalGrid.addAll(clusteredOcr);
        }

        if (finalGrid.size() < 5)
        {
            String xml = HSQTools.getFlexibleXML();
            finalGrid.addAll(convertXmlToTextBlocks(xml));
        }

        return finalGrid;
    }

    private static com.google.mlkit.vision.text.TextRecognizer getSharedTextRecognizer()
    {
        if (textRecognizer == null)
        {
            // Chỉ khởi tạo 1 lần duy nhất
            textRecognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                    com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
            );
        }
        return textRecognizer;
    }
    /**
     * Tìm tất cả các ô Radio/Check/View có thể click nằm trên cùng dòng Y với Row Label
     */
    private java.util.List<android.graphics.Rect> findMatrixElementsInRow(String xml, int rowY)
    {
        java.util.List<android.graphics.Rect> rowElements = new java.util.ArrayList<>();
        try
        {
            // Regex tìm các node có class nút bấm hoặc clickable=true
            Pattern pattern = Pattern.compile("<node.*?class=\"([^\"]*)\".*?clickable=\"true\".*?bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"");
            Matcher matcher = pattern.matcher(xml);
            while (matcher.find())
            {
                int left = Integer.parseInt(matcher.group(2));
                int top = Integer.parseInt(matcher.group(3));
                int right = Integer.parseInt(matcher.group(4));
                int bottom = Integer.parseInt(matcher.group(5));
                int centerY = (top + bottom) / 2;

                // Chốt chặn: Phải nằm cùng dòng với Row Label (sai số 50px)
                if (Math.abs(centerY - rowY) <= 50)
                {
                    rowElements.add(new android.graphics.Rect(left, top, right, bottom));
                }
            }
            // Sắp xếp từ trái qua phải để tính đúng cột 1, 2, 3...
            rowElements.sort(Comparator.comparingInt(r -> r.left));
        }
        catch (Exception ignored)
        {
        }
        return rowElements;
    }
    // ==========================================
// 1. MÁY GIẶT OPENCV: Tẩy nền xám thành Trắng/Đen
// ==========================================
    private Bitmap preprocessImageForMLKit(Bitmap rawScreenshot) {
        if (rawScreenshot == null) return null;

        Mat mat = new Mat();
        // Chuyển Bitmap thành Ma trận OpenCV
        Utils.bitmapToMat(rawScreenshot, mat);

        // Chuyển ảnh sang hệ màu Xám (Grayscale)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);

        // 🌟 PHÉP THUẬT OTSU: Tự động tính toán độ xám,
        // ép TẤT CẢ nền thành giấy trắng, TẤT CẢ chữ thành mực đen!
        Imgproc.threshold(mat, mat, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        // Tạo Bitmap mới từ ma trận đã tẩy trắng
        Bitmap cleanBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, cleanBitmap);

        // Giải phóng bộ nhớ OpenCV
        mat.release();

        return cleanBitmap;
    }

    // ==========================================
// 2. MẮT THẦN ML KIT: Đọc chữ tốc độ bàn thờ
// ==========================================
    private List<HSQTools.TextBlock> getOcrTextBlocks() {
        List<HSQTools.TextBlock> list = new ArrayList<>();

        // Chụp màn hình
        Bitmap rawScreenshot = HSQTools.getScreenBitmap();
        if (rawScreenshot == null) return list;

        Bitmap cleanScreenshot = null;
        try {
            updateNotificationContent("Đang nhúng màn hình vào máy giặt OpenCV...");

            // 1. Đưa ảnh qua máy giặt OpenCV để tẩy nền xám
            cleanScreenshot = preprocessImageForMLKit(rawScreenshot);
            if (cleanScreenshot == null) return list;

            updateNotificationContent("ML Kit đang cào data...");

            // 2. Khởi tạo ML Kit (Nhận diện ký tự Latinh/Tiếng Việt chuẩn)
            InputImage image = InputImage.fromBitmap(cleanScreenshot, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            // 3. Xử lý đồng bộ (Ép Tool chờ ML Kit đọc xong mới chạy tiếp)
            Task<Text> resultTask = recognizer.process(image);
            Text result = Tasks.await(resultTask); // Chờ kết quả trả về

            // 4. Bóc tách Dữ liệu (BẮT BUỘC LẤY LINE, KHÔNG LẤY BLOCK ĐỂ TRÁNH DÍNH CỘT)
            if (result != null && result.getTextBlocks() != null) {
                for (Text.TextBlock block : result.getTextBlocks()) {
                    String text = block.getText().replace("\n", " ").replaceAll("\\s+", " ");
                    Rect boundingBox = block.getBoundingBox(); // Lấy khung Box

                    if (boundingBox != null && text != null && !text.isEmpty()) {
                        // Lấy Tâm X, Tâm Y chuẩn xác để chọt Auto
                        int cx = boundingBox.centerX();
                        int cy = boundingBox.centerY();

                        list.add(new HSQTools.TextBlock(text.trim(), cx, cy));
                    }
                }
            }
        } catch (Exception e) {
            updateNotificationContent("Lỗi OCR Tối Thượng: " + e.getMessage());
            delay(3000);
        } finally {
            // Dọn dẹp rác Bitmap để không bị tràn RAM (Out Of Memory)
            if (rawScreenshot != null && !rawScreenshot.isRecycled()) {
                rawScreenshot.recycle();
            }
            if (cleanScreenshot != null && !cleanScreenshot.isRecycled()) {
                cleanScreenshot.recycle();
            }
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
            Pattern pattern = Pattern.compile("<node.*?class=\"([^\"]*?)\".*?checkable=\"true\".*?bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"");
            Matcher matcher = pattern.matcher(xml);
            while (matcher.find())
            {
                int left = Integer.parseInt(matcher.group(2));
                int top = Integer.parseInt(matcher.group(3));
                int right = Integer.parseInt(matcher.group(4));
                int bottom = Integer.parseInt(matcher.group(5));
                if (top > minTop && top < 2800)
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

                // GỬI BÁO CÁO VIP LÊN TELEGRAM
                HSQTools.sendTelegramAlertVIP(deviceID, "LỖI LỲ LỢM [" + actionType.toUpperCase() + "]: " + stepDetail, idTelegram, imagePath + "/screenCap.png", currentXml);

                while (true)
                {
                    updateNotificationContent("Kẹt " + actionType + ": Chờ sếp xử lý tay...");
                    List<HSQTools.TextBlock> act = getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList());
                    if (!HSQTools.areAlmostSame(currentVisible, act, 20)) break;
                    delay(180000);
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
                show();
                delay(2000);
                updateContent("click trượt " + x + " " + y);
                delay(10000);
                hide();
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
                show();
                delay(2000);
                updateContent("lỗi inputText");
                delay(10000);
                hide();
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
                show();
                delay(2000);
                updateContent("lỗi swipe");
                delay(10000);
                hide();
            }
        }
    }

    public static android.graphics.Rect FindNextButtonBoundsFromXmlString(String xml)
    {
        if (xml == null || xml.trim().isEmpty()) return null;
        try
        {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

            java.util.List<android.graphics.Rect> candidates = new java.util.ArrayList<>();

            // 🌟 Regex 1: Khớp CHÍNH XÁC toàn bộ chuỗi (Không thừa 1 ký tự nào) - An toàn tuyệt đối
            java.util.regex.Pattern patternExact = java.util.regex.Pattern.compile("^(continue|next|submit|tieptuc|tieptheo|tieptheo>|done|gui|send|batdau|agree|accept|agreeandcontinue|>|>>|>>>|gotonextquestion|fwd|forward|tiep)$");
            // 🌟 Regex 2: Khớp CHỨA (Chỉ dùng khi xác nhận nó là Button) - Nới lỏng một chút
            java.util.regex.Pattern patternContains = java.util.regex.Pattern.compile("(continue|next|submit|tieptuc|tieptheo|done|gui|send|batdau|agree|accept|agreeandcontinue|gotonextquestion|fwd|forward|tiep)");
            for (int i = 0; i < nodes.getLength(); i++)
            {
                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                String text = node.getAttribute("text");
                String desc = node.getAttribute("content-desc");
                String clazz = node.getAttribute("class");
                String resId = node.getAttribute("resource-id");

                String combined = (text + " " + desc).toLowerCase();
                String cleanText = HSQTools.removeVietnameseDiacriticsAndWhitespace(combined).replaceAll("[^a-z0-9>]", "");
                String cleanResId = resId != null ? resId.toLowerCase() : "";

                // 🔥 ĐIỀU KIỆN 1: Bắt theo Resource ID (CHẮC ĂN 100%, đéo sợ nhầm tiêu đề)
                boolean isResIdMatch = cleanResId.contains("forwardbutton")
                        || cleanResId.endsWith(":id/next")
                        || cleanResId.contains("btn_next")
                        || cleanResId.contains("continue_button")
                        || cleanResId.endsWith(":id/submit")
                        || cleanResId.contains("gtm-agree-button")
                        || cleanResId.contains("fwd")
                        || cleanResId.contains("forward")
                        || cleanResId.equals("nextbutton"); // 🌟 Qualtrics xài thẳng ID NextButton này

                // 🔥 ĐIỀU KIỆN 2: Xác nhận là THẺ BUTTON xịn
                boolean isButton = clazz != null && clazz.contains("Button");
                boolean isClassMatch = isButton && (patternContains.matcher(cleanText).find() || cleanText.equals("tiep"));

                // 🔥 ĐIỀU KIỆN 3: Chữ khớp CHÍNH XÁC tuyệt đối
                boolean isExactTextMatch = patternExact.matcher(cleanText).matches();

                if (isResIdMatch || isClassMatch || isExactTextMatch)
                {
                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                    // 🚫 LƯỚI LỌC CHỐNG CẢNH BÁO / TIÊU ĐỀ
                    // - Nút Next luôn nằm ở nửa dưới (y > 800)
                    // - Không có cái nút nào cao quá 250px (chặn bắt nhầm Layout chứa chữ)
                    // - Phải có chiều rộng đàng hoàng (width > 10) để chống Node Bóng Ma
                    if (r != null && r.top > 800 && r.height() < 250 && r.width() > 10)
                    {
                        candidates.add(r);
                    }
                }
            }

            if (!candidates.isEmpty())
            {
                // Sắp xếp lấy cái nút nằm DƯỚI CÙNG của màn hình (Vì Next thường chốt hạ ở đáy)
                candidates.sort((a, b) -> Integer.compare(b.top, a.top));
                return candidates.get(0);
            }
        }
        catch (Exception ignored)
        {
        }
        return null;
    }

    // =========================================================================
    // 🎯 RADAR SĂN CÂU HỎI (Tách riêng cho gọn, có ưu tiên dấu hỏi chấm)
    // =========================================================================
    public static String findHeaderFromText(java.util.List<String> allTextScanned)
    {
        if (allTextScanned == null || allTextScanned.isEmpty())
        {
            return "";
        }

        for (String text : allTextScanned)
        {
            String lowerText = text.toLowerCase().trim();

            // 1. LƯỚI LỌC RÁC CỨNG (Bỏ qua các thành phần UI, nút bấm, hướng dẫn...)
            if (lowerText.length() < 5 ||
                    lowerText.equals("back button") ||
                    lowerText.contains("offerwall") ||
                    lowerText.matches(".*\\d+%.*hoàn thành.*") || // Chặn "4% Hoàn thành khảo sát"
                    lowerText.contains("ngôn ngữ") ||
                    lowerText.contains("language") ||
                    lowerText.contains("tiếng việt") ||
                    lowerText.contains("english") ||
                    lowerText.contains("trang tiếp theo") ||
                    lowerText.contains("được cung cấp bởi"))
            {
                continue; // Lướt qua, xét dòng tiếp theo!
            }

            // 2. 🌟 ƯU TIÊN TỐI CAO: DẤU HỎI CHẤM (?) Ở CUỐI CÂU
            // Nếu dòng chữ kết thúc bằng dấu '?', 99% nó là câu hỏi. Chốt đơn ngay lập tức!
            if (text.trim().endsWith("?"))
            {
                Log.d("Radar", "Tóm được Header nhờ dấu chấm hỏi: " + text);
                return text;
            }

            // 3. DÙNG NÃO AI LOCAL ĐỂ KIỂM TRA NGƯỢC (Fallback)
            // Nếu câu hỏi không có dấu '?' (ví dụ: "Vui lòng chọn giới tính"), ta nhờ AI nếm thử.
            String testIntent = CompactAIHelper.classifyQuestion(text);
            if (!testIntent.equals("khac"))
            {
                Log.d("Radar", "Tóm được Header nhờ AI nếm thử: " + text + " (Intent: " + testIntent + ")");
                return text;
            }

            // 4. LƯỚI DỰ PHÒNG CUỐI CÙNG (Regex đặc trưng)
            if (lowerText.contains("vui lòng") ||
                    lowerText.contains("bạn đang") ||
                    lowerText.contains("anh/chị") ||
                    lowerText.contains("chọn một") ||
                    lowerText.contains("hãy cho biết"))
            {
                Log.d("Radar", "Tóm được Header nhờ Regex: " + text);
                return text;
            }
        }

        // Nếu vuốt sạch mảng mà đéo có dòng nào lọt qua các lưới lọc trên thì đành trả về rỗng
        return "";
    }

    // =========================================================================
    // 🧠 RADAR TÌM CÂU HỎI CHƯA TRẢ LỜI DÀNH CHO LOCAL AI (BẢN GỌT RÁC ASBL)
    // =========================================================================
    private String findUnansweredHeaderForLocal(List<HSQTools.TextBlock> visibleTexts, String xmlDump)
    {
        // 1. Trích xuất tọa độ Y của tất cả các đáp án đã được CHỌN (checked="true")
        java.util.List<Integer> checkedYList = new java.util.ArrayList<>();
        java.util.regex.Matcher mChecked = java.util.regex.Pattern.compile("<node[^>]*?checked=\"true\"[^>]*?bounds=\"\\[\\d+,(\\d+)\\]\\[\\d+,\\d+\\]\"").matcher(xmlDump);
        while (mChecked.find())
        {
            checkedYList.add(Integer.parseInt(mChecked.group(1)));
        }

        visibleTexts.sort(java.util.Comparator.comparingInt(x -> x.y));

        int answeredCount = 0;
        int totalQuestionsFound = 0;

        for (int i = 0; i < visibleTexts.size(); i++)
        {
            HSQTools.TextBlock tb = visibleTexts.get(i);
            String rawText = tb.text;

            // 🔥 CÚ ĐÁNH CHẶN 1: Gọt sạch rác ASBL (dấu *, Được yêu cầu, Required)
            // Dùng (?i) để regex không phân biệt chữ hoa/thường
            String cleanText = rawText.replaceAll("(?i)Được yêu cầu|Required|Bắt buộc", "")
                    .replace("*", "")
                    .trim();

            String lowerText = cleanText.toLowerCase();

            // 🔥 CÚ ĐÁNH CHẶN 2: Đồng bộ não CompactAIHelper để không bị trượt như trước
            boolean isQuestion = cleanText.endsWith("?") ||
                    lowerText.contains("vui lòng") ||
                    lowerText.contains("chọn một") ||
                    !CompactAIHelper.classifyQuestion(cleanText).equals("khac"); // Nếm thử bằng AI

            if (isQuestion && lowerText.length() > 5)
            {
                totalQuestionsFound++;
                int currentQuestionY = tb.y;
                int nextQuestionY = currentQuestionY + 1200;

                // Tìm giới hạn Y của câu hỏi tiếp theo
                for (int j = i + 1; j < visibleTexts.size(); j++)
                {
                    String nextRaw = visibleTexts.get(j).text;
                    String nextClean = nextRaw.replaceAll("(?i)Được yêu cầu|Required|Bắt buộc", "").replace("*", "").trim().toLowerCase();

                    if (nextClean.endsWith("?") || nextClean.contains("vui lòng") || nextClean.contains("chọn một") || !CompactAIHelper.classifyQuestion(nextClean).equals("khac"))
                    {
                        nextQuestionY = visibleTexts.get(j).y;
                        break;
                    }
                }

                // 3. Kiểm tra xem trong vùng của câu hỏi này, đã có đáp án nào được check chưa?
                boolean isAnswered = false;
                for (int checkY : checkedYList)
                {
                    if (checkY > currentQuestionY && checkY < nextQuestionY)
                    {
                        isAnswered = true;
                        break;
                    }
                }

                if (isAnswered)
                {
                    answeredCount++;
                    continue;
                }
                else
                {
                    // 🌟 TRẢ VỀ TEXT ĐÃ ĐƯỢC GỌT RÁC SẠCH SẼ CHO LOCAL BRAIN ĐỌC
                    return cleanText;
                }
            }
        }

        if (totalQuestionsFound > 0 && totalQuestionsFound == answeredCount)
        {
            return "ALL_ANSWERED_CLICK_NEXT";
        }

        return "";
    }

    // =======================================================
// THUẬT TOÁN GOM CỤM THÔNG MINH (PHIÊN BẢN CHỐNG CHIA CỘT)
// =======================================================
    private List<HSQTools.TextBlock> clusterTextBlocks(List<HSQTools.TextBlock> originalList) {
        List<HSQTools.TextBlock> clusteredList = new ArrayList<>();
        if (originalList == null || originalList.isEmpty()) return clusteredList;

        // 1. Sort theo Y từ trên xuống dưới
        java.util.Collections.sort(originalList, new java.util.Comparator<HSQTools.TextBlock>() {
            @Override
            public int compare(HSQTools.TextBlock b1, HSQTools.TextBlock b2) {
                return Integer.compare(b1.y, b2.y);
            }
        });

        List<HSQTools.TextBlock> currentLine = new ArrayList<>();
        currentLine.add(originalList.get(0));

        // 🌟 Sai số Y để nhận diện cùng 1 hàng (Nới lỏng lên 45px để ăn trọn các bảng Web bị lệch)
        int yTolerance = 45;

        for (int i = 1; i < originalList.size(); i++) {
            HSQTools.TextBlock currentBlock = originalList.get(i);
            HSQTools.TextBlock firstBlockInLine = currentLine.get(0);

            if (Math.abs(currentBlock.y - firstBlockInLine.y) <= yTolerance) {
                currentLine.add(currentBlock);
            } else {
                clusteredList.add(mergeLineBlocks(currentLine));
                currentLine.clear();
                currentLine.add(currentBlock);
            }
        }
        if (!currentLine.isEmpty()) {
            clusteredList.add(mergeLineBlocks(currentLine));
        }

        return clusteredList;
    }

    private HSQTools.TextBlock mergeLineBlocks(List<HSQTools.TextBlock> lineBlocks) {
        if (lineBlocks.size() == 1) {
            // Tẩy luôn \n nếu chỉ có 1 khối
            String cleanSingle = lineBlocks.get(0).text.replace("\n", " ").replaceAll("\\s+", " ").trim();
            return new HSQTools.TextBlock(cleanSingle, lineBlocks.get(0).x, lineBlocks.get(0).y);
        }

        // Sort theo X từ Trái sang Phải
        java.util.Collections.sort(lineBlocks, new java.util.Comparator<HSQTools.TextBlock>() {
            @Override
            public int compare(HSQTools.TextBlock b1, HSQTools.TextBlock b2) {
                return Integer.compare(b1.x, b2.x);
            }
        });

        StringBuilder combinedText = new StringBuilder();
        int sumX = 0, sumY = 0;

        for (int i = 0; i < lineBlocks.size(); i++) {
            HSQTools.TextBlock b = lineBlocks.get(i);
            // 🌟 Tẩy sạch \n và nối chữ mượt mà
            combinedText.append(b.text.replace("\n", " ").replaceAll("\\s+", " ").trim());
            if (i < lineBlocks.size() - 1) {
                combinedText.append(" ");
            }
            sumX += b.x;
            sumY += b.y;
        }

        int avgX = sumX / lineBlocks.size();
        int avgY = sumY / lineBlocks.size();

        return new HSQTools.TextBlock(combinedText.toString().trim(), avgX, avgY);
    }
}


