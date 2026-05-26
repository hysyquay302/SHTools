package com.quayquay.shtools;

import static com.quayquay.shtools.extention.ASUtils.delay;
import static com.quayquay.shtools.services.ASBLBridgeService.clearrecents;
import static com.quayquay.shtools.services.ASBLBridgeService.findAndClickByTextDes;
import static com.quayquay.shtools.services.ASBLBridgeService.globalBack;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.android.gms.tasks.Tasks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.quayquay.hsq.tools.GeminiApiHelper;
import com.quayquay.hsq.tools.HSQConfig;
import com.quayquay.hsq.tools.HSQDevice;
import com.quayquay.hsq.tools.HSQFacebook;
import com.quayquay.hsq.tools.HSQFileHelper;
import com.quayquay.hsq.tools.HSQHttps;
import com.quayquay.hsq.tools.HSQRoot;
import com.quayquay.hsq.tools.HSQService;
import com.quayquay.hsq.tools.HSQTools;
import com.quayquay.hsq.tools.HSQTools.TextBlock;
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
    private static final String apiKeyServer = BuildConfig.API_SERVEY;
    private static final String apiKeyGem = BuildConfig.API_KEY_GEMINI_KEY;
    private GeminiApiHelper geminiAI;
    private static final int widthOfScreen = ASBLBridgeService.widthOfScreen;
    private static final int heightOfScreen = ASBLBridgeService.heightOfScreen;
    private static final int dpi = ASBLBridgeService.dpi;
    static AccessibilityService asblService = ASBLBridgeService.asblService;

    private static int apkVersion = 0;
    private static int remotePromtVersion = 0;
    private String localServerIp = "";
    private String apiGemini = "";
    private String idTelegram = "";
    private String customAgentRule = "";

    private static final int VCode = BuildConfig.VERSION_CODE;
    public static final String deviceID = HSQTools.getDeviceSerial(HSQConfig.getContext());
    private static final String shortDeviceID = getShortDeviceID();
    @SuppressLint("SdCardPath")
    private final String imagePath = "/sdcard/Pictures/ImageChat";

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

    public static boolean isStop = false;

    @SuppressLint("SdCardPath")

    @Override
    public void onStarted(JSONObject object)
    {
        try
        {
            //Truyền service ASBL của sếp cho thư viện
            com.quayquay.hsq.tools.HSQConfig.setASBLService(ASBLBridgeService.asblService);
            //CẮM DÂY CLICK: Chuyền tọa độ từ Thư viện sang cho hàm click của sếp tự múa!
            com.quayquay.hsq.tools.HSQConfig.setASBLBridge(ASBLBridgeService::do_click);
            apiGemini = getNewApiGeminiKey();
            geminiAI = new GeminiApiHelper(HSQConfig.getContext(), apiGemini, false);
            HSQTools.setIsRooted(false);
            HSQTools.setIsAdminApp(true);
            String directionPath = "/sdcard/Servey/direction.json";
            String pathInfoProfileSaved = "/sdcard/Servey/sv_" + deviceID + ".json";
            String baseRule = HSQFileHelper.readTextFile("/sdcard/Servey/PromtGem.txt");
            RegistrationInfo InfoProfile;
            String profileData;
            while (true)
            {
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
            Instant startTime = Instant.now();
            startTool:
            while (true)
            {
                hide();
                updateTitle(shortDeviceID);
                updateNotificationTitle(shortDeviceID);
                updateNotificationContent("Ready...");
                delay(2000);
                loadControl();

                updateNotificationContent("Start...");

                List<HSQTools.TextBlock> lastScreen;
                List<HSQTools.TextBlock> AllPointsOK = new ArrayList<>();
                Map<String, Integer> matrixColumnCache = new HashMap<>();

                HSQFileHelper.createFolder(deviceID, imagePath);
                delay(1000);
                beginApp:
                while (true)
                {
                    int LastInterFace = 0, screenSwipe = 0, xs = 720, yTop = 1000, yBot = 2500, tempSwipeCount = 0, clickChoose = 0, scanFull, checkloi = 0,
                            swipeDuration = 0;
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
                            int checkServey = HSQTools.getImageExistss(20, true, R.drawable.btr_accept, R.drawable.btr_serveysbl_click, R.drawable.btr_serveysbl_click_1);
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
                                delay(5000);
                                continue;
                            }

                            delay(3000);
                            updateNotificationContent("check servey");

                            if (HSQTools.getImageExistss(2, false, R.drawable.btr_minutes) == 0)
                            {
                                updateNotificationContent("không có servey, check lại sau 1 phút");
                                delay(60000);
                                HSQTools.getImageExistss(2, true, R.drawable.btr_refreshservey);
                                delay(5000);
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
                                        else click(720, 1500, false);
                                        delay(5000);
                                        continue startTool;
                                    }

                                    if (w % 3 == 0) swipe(720, 1500, 720, 1350, 300);
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
                                        int minX = (col == 0) ? 0 : 720;
                                        int maxX = (col == 0) ? 720 : 2500;

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
                                    swipe(720, 2600, 720, 1000, 1500);
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
                                            swipe(720, 1200, 720, 2800, 1500);
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
                                            swipe(720, 2600, 720, 1000, 1500);
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
                                R.drawable.btr_complete_profile, R.drawable.btr_wefound, R.drawable.btr_profile_match, R.drawable.btr_accept
                        );
                        if (checkPRF == 4)
                        {
                            HSQTools.getImageExistss(2, true, R.drawable.btr_accept);
                            delay(5000);
                            continue;
                        }
                        else if (checkPRF == 1)
                        {
                            int checkSetup = HSQTools.getImageExistss(
                                    2, false,
                                    R.drawable.btr_english, R.drawable.btr_gioitinh, R.drawable.btr_zipcode, R.drawable.btr_thunhaptrungbinhgiadinhhangnam_1, R.drawable.btr_thunhaptrungbinhgiadinhhangnam,
                                    R.drawable.btr_ttvl, R.drawable.btr_treduoi18, R.drawable.btr_xacnhanmail, R.drawable.btr_start_servey, R.drawable.btr_accept
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
                                click(704, 2803, false); // continue
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
                                click(704, 2803, false); // continue
                            }//zip code
                            else if (checkSetup == 4 && LastInterFace != 4)
                            {
                                updateNotificationContent("Thu nhập trung bình HGĐ hàng năm");
                                while (true)
                                {
                                    if (HSQTools.getImageExistss(2, true, R.drawable.btr_tren60trieu) != 0)
                                    {
                                        break;
                                    }
                                    swipe(720, 2400, 720, 1000, 1500);
                                    delay(2000);
                                }
                                delay(2000);
                                click(704, 2803, false); // continue
                            }// thu nhập gia đình hàng năm select
                            else if (checkSetup == 5 && LastInterFace != 5)
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
                                click(704, 2803, false); // continue
                            }// thu nhập gia đình hàng năm input
                            else if (checkSetup == 6 && LastInterFace != 6)
                            {
                                updateNotificationContent("TTVL");
                                while (HSQTools.getImageExistss(2, true, R.drawable.btr_ttvl_toanthoigian) == 0)
                                {
                                    swipe(720, 2613, 720, 800, 1500);
                                    HSQTools.delay(2000);
                                }
                                HSQTools.delay(2000);
                                click(704, 2803, false); // continue
                            }// thông tin việc làm
                            else if (checkSetup == 7 && LastInterFace != 7)
                            {
                                updateNotificationContent("trẻ dưới 18");
                                while (HSQTools.getImageExistss(2, true, R.drawable.btr_treduoi18_1be) == 0)
                                {
                                    swipe(720, 2613, 720, 800, 1500);
                                    HSQTools.delay(2000);
                                }
                                HSQTools.delay(3000);
                                click(704, 2803, false); // continue
                            }// số lượng trẻ dưới 18
                            else if (checkSetup == 8 && LastInterFace != 8)
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
                                click(704, 2803, false); // continue
                            }// nhập lại email
                            else if (checkSetup == 9)
                            {
                                HSQTools.getImageExistss(2, true, R.drawable.btr_start_servey);
                                HSQTools.delay(10000);
                                break; // (profile match)
                            }// start servey
                            else if (checkSetup == 10)
                            {
                                HSQTools.getImageExistss(2, true, R.drawable.btr_accept);
                            }//accept
                            else
                            {
                                delay(1000);
                            }

                            LastInterFace = checkSetup;
                            if (checkSetup != 0 && checkSetup < 11)
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
                                    R.drawable.btr_toisinhra, R.drawable.btr_tinh_sv, R.drawable.btr_serveysbl, R.drawable.btr_serveysbl_1, R.drawable.btr_complete_profile
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
                            else if (checkGDKS == 0 || checkGDKS == 9 || checkGDKS == 10 || checkGDKS == 12 || checkGDKS == 15 || checkGDKS == 16)
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
                                                xs = 720;
                                                yTop = 1100;
                                                yBot = 2400;
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
                                                swipe(xs, yTop, xs, yBot, swipeDuration);
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
                                                    swipe(xs, yTop, xs, yBot, swipeDuration);
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
                                                if (screenSwipe == 0)
                                                {
                                                    String currentXmlForSwipe = getFlexibleXML();
                                                    android.graphics.Rect dropBounds = HSQTools.findActiveDropdownBounds(currentXmlForSwipe);

                                                    // 🔥 TRIỆT TIÊU LỖI NHẬN NHẦM KHUNG CẢNH BÁO / HEADER:
                                                    // 1. Hộp Dropdown xịn chiều cao bắt buộc phải lớn (height > 400px)
                                                    // 2. Né hoàn toàn nếu màn hình đang chứa cấu trúc bảng Ma trận (RadioButton)
                                                    if (dropBounds != null && dropBounds.height() > 400 && !currentXmlForSwipe.contains("RadioButton"))
                                                    {
                                                        xs = dropBounds.left + (dropBounds.width() / 2);
                                                        yBot = dropBounds.bottom - 50;
                                                        yTop = dropBounds.top + 50;

                                                        if (yBot > 2600 && (yBot - yTop > 300))
                                                            yBot = 2600;
                                                        if (yTop < 400) yTop = 400;

                                                        int distance = Math.abs(yBot - yTop);
                                                        swipeDuration = Math.max(350, Math.min(1500, distance));
                                                    }
                                                    else
                                                    {
                                                        xs = 720;
                                                        yTop = 1000;
                                                        yBot = 2500;
                                                        swipeDuration = 1500;
                                                    }
                                                }

                                                delay(1000);
                                                HSQFileHelper.deleteFile(imagePath);
                                                HSQFileHelper.createFolder(imagePath);
                                                delay(1000);
                                                HSQTools.captureAndSaveScreen(imagePath + "/screenCapa1.png");
                                                delay(1000);
                                                while (true)
                                                {
                                                    List<HSQTools.TextBlock> beforeSwipe = getScreenText().stream()
                                                            .filter(x -> x.y > 180).collect(Collectors.toList());
                                                    swipe(xs, yBot, xs, yTop, swipeDuration);
                                                    delay(3000);

                                                    screenBegin = getScreenText().stream()
                                                            .filter(x -> x.y > 180).collect(Collectors.toList());

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
                                                                    boolean answerOK = false;
                                                                    List<TextBlock> temp = new ArrayList<>();
                                                                    int vuotLenLai = 0, checkLaiScreen = 0;

                                                                    timTextLoop:
                                                                    while (true)
                                                                    {
                                                                        List<TextBlock> checkAnswer = getCheckAnswerSmart().stream()
                                                                                .filter(x -> x.y > 180 && x.y < 2750).collect(Collectors.toList());

                                                                        while (true)
                                                                        {
                                                                            // Logic chống kẹt màn hình của sếp
                                                                            if (HSQTools.areAlmostSame(temp, checkAnswer, 20))
                                                                            {
                                                                                if (vuotLenLai == 0)
                                                                                {
                                                                                    vuotLenLai++;
                                                                                    swipe(xs, yTop, xs, yBot, swipeDuration);
                                                                                    delay(2000);
                                                                                }
                                                                                else
                                                                                {
                                                                                    handleActionFailure(
                                                                                            "clicktotext", step, checkAnswer,
                                                                                            "Mày bảo tao click vào [" + step + "] nhưng tao vuốt từ đỉnh đến đáy đéo thấy chữ đó đâu, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời"
                                                                                    );
                                                                                    continue stateMachine;
                                                                                }
                                                                            }

                                                                            // 🎯 TẦNG 0.5: TRUY QUÉT BẰNG XML (ĐẶC TRỊ OCR BỊ MÙ)
                                                                            // Bọn Webview hay làm OCR mù, nhưng XML thì không bao giờ biết nói dối!
                                                                            // --------------------------------------------------------
                                                                            String currentXml = HSQTools.getFlexibleXML();
                                                                            try
                                                                            {
                                                                                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                for (int i = 0; i < nodes.getLength(); i++)
                                                                                {
                                                                                    org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                    String text = node.getAttribute("text");
                                                                                    String desc = node.getAttribute("content-desc");
                                                                                    String combinedXmlText = HSQTools.getOnlyTextLinq(normalizeText(text + " " + desc));

                                                                                    // Nếu XML bắt được chữ khớp tuyệt đối
                                                                                    if (!combinedXmlText.isEmpty() && combinedXmlText.equals(resultNorm))
                                                                                    {
                                                                                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                                                                                        if (r != null && r.height() > 0 && r.height() < 300)
                                                                                        {

                                                                                            // TRƯỜNG HỢP 1: Node xịn, có chiều rộng đàng hoàng
                                                                                            if (r.width() > 10 && r.centerX() > 10)
                                                                                            {
                                                                                                updateNotificationContent("XML Xịn! Tóm được [" + resultNorm + "] tại X=" + r.centerX() + ", Y=" + r.centerY());
                                                                                                click(r.centerX(), r.centerY(), false);
                                                                                                previousText = resultNorm;
                                                                                                break timTextLoop;
                                                                                            }
                                                                                            // TRƯỜNG HỢP 2: Node Bóng Ma (Tọa độ bị bóp về X = 0)
                                                                                            else
                                                                                            {
                                                                                                // Gọi hồn thằng Node em nằm kế tiếp nó trong XML
                                                                                                if (i + 1 < nodes.getLength())
                                                                                                {
                                                                                                    org.w3c.dom.Element nextNode = (org.w3c.dom.Element) nodes.item(i + 1);
                                                                                                    android.graphics.Rect nextR = HSQTools.parseBoundsFromXml(nextNode.getAttribute("bounds"));

                                                                                                    // Nếu thằng em có tọa độ đàng hoàng và nằm ngang hàng với Bóng Ma (sai số 50px)
                                                                                                    if (nextR != null && nextR.width() > 10 && nextR.centerX() > 10 && Math.abs(nextR.centerY() - r.centerY()) < 50)
                                                                                                    {
                                                                                                        updateNotificationContent("Bóng Ma XML! Mượn thân xác thằng kế bên: " + nextR.centerX() + "," + nextR.centerY());
                                                                                                        click(nextR.centerX(), nextR.centerY(), false);
                                                                                                        previousText = resultNorm;
                                                                                                        break timTextLoop;
                                                                                                    }
                                                                                                }

                                                                                                // TRƯỜNG HỢP 3: Cùng đường (Bóng ma cô đơn) -> Ép chọt lụi theo trục Y, X dời ra 250 cho an toàn
                                                                                                updateNotificationContent("Bóng ma cô đơn! Ép chọt X=250, Y=" + r.centerY());
                                                                                                click(250, r.centerY(), false);
                                                                                                previousText = resultNorm;
                                                                                                break timTextLoop;
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                            catch (Exception ignored)
                                                                            {
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
                                                                                // Vì là Equals 100% nên nếu có trùng chữ (VD: 2 chữ "Nam" trên màn), cứ lấy thằng ở trên (min Y)
                                                                                TextBlock target = (candidates1.size() == 1) ? candidates1.get(0) :
                                                                                        candidates1.stream().min(Comparator.comparingInt(c -> c.y)).orElse(null);
                                                                                if (target != null)
                                                                                {
                                                                                    click(target.x, target.y, false);
                                                                                    previousText = resultNorm;
                                                                                    break timTextLoop; // Dứt điểm, thoát khối!
                                                                                }
                                                                            }

                                                                            // --------------------------------------------------------
                                                                            // 🎯 TẦNG 2: KHỚP CHỨA (CONTAINS) - CHỐT LUÔN!
                                                                            // 🔥 BỌC THÉP: Nếu chuỗi chứa quá nhiều chữ số (Tiền bạc, Năm sinh), CẤM dùng Contains để tránh bắt nhầm dải số!
                                                                            // --------------------------------------------------------
                                                                            long digitCount = resultNorm.chars().filter(Character::isDigit).count();

                                                                            if (digitCount < 3)
                                                                            {
                                                                                List<TextBlock> candidates2 = new ArrayList<>();
                                                                                for (TextBlock answer : checkAnswer)
                                                                                {
                                                                                    String answerChoose = HSQTools.getOnlyTextLinq(normalizeText(answer.text));
                                                                                    if (answerChoose.contains(resultNorm) || (answerChoose.length() >= 5 && resultNorm.contains(answerChoose)))
                                                                                    {
                                                                                        candidates2.add(answer);
                                                                                    }
                                                                                }

                                                                                if (!candidates2.isEmpty())
                                                                                {
                                                                                    final String finalResultNorm = resultNorm;
                                                                                    TextBlock target = (candidates2.size() == 1) ? candidates2.get(0) :
                                                                                            candidates2.stream().min((c1, c2) ->
                                                                                            {
                                                                                                int dist1 = HSQTools.levenshtein(HSQTools.getOnlyTextLinq(normalizeText(c1.text)), finalResultNorm);
                                                                                                int dist2 = HSQTools.levenshtein(HSQTools.getOnlyTextLinq(normalizeText(c2.text)), finalResultNorm);
                                                                                                return (dist1 == dist2) ? Integer.compare(c1.y, c2.y) : Integer.compare(dist1, dist2);
                                                                                            }).orElse(null);
                                                                                    if (target != null)
                                                                                    {
                                                                                        click(target.x, target.y, false);
                                                                                        previousText = resultNorm;
                                                                                        break timTextLoop;
                                                                                    }
                                                                                }
                                                                            }

                                                                            // --------------------------------------------------------
                                                                            // 🎯 TẦNG 3: KHỚP SỰ KIỆN SAI SỐ (LEVENSHTEIN) - CHỐT LUÔN!
                                                                            // 🔥 BỌC THÉP: Cấm tương tự Tầng 2
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
                                                                                            candidates3.stream().min((c1, c2) ->
                                                                                            {
                                                                                                int dist1 = HSQTools.levenshtein(HSQTools.getOnlyTextLinq(normalizeText(c1.text)), finalResultNorm);
                                                                                                int dist2 = HSQTools.levenshtein(HSQTools.getOnlyTextLinq(normalizeText(c2.text)), finalResultNorm);
                                                                                                return (dist1 == dist2) ? Integer.compare(c1.y, c2.y) : Integer.compare(dist1, dist2);
                                                                                            }).orElse(null);
                                                                                    if (target != null)
                                                                                    {
                                                                                        click(target.x, target.y, false);
                                                                                        previousText = resultNorm;
                                                                                        break timTextLoop;
                                                                                    }
                                                                                }
                                                                            }
                                                                            // --------------------------------------------------------
                                                                            // 🎯 TẦNG DỰ PHÒNG: LOGIC ĐẶC BIỆT NỮ/FEMALE CỦA SẾP
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

                                                                            // Logic Fallback vuốt màn hình khi cả 3 tầng đều mù hoàn toàn
                                                                            if (checkLaiScreen == 0)
                                                                            {
                                                                                checkAnswer = getCheckAnswerSmart().stream().filter(x -> x.y > 180 && x.y < 2750).collect(Collectors.toList());
                                                                                checkLaiScreen++;
                                                                                continue;
                                                                            }
                                                                            checkLaiScreen = 0;
                                                                            temp = checkAnswer;
                                                                            if (vuotLenLai == 0)
                                                                            {
                                                                                swipe(xs, yBot, xs, yTop, swipeDuration);
                                                                            }
                                                                            else
                                                                            {
                                                                                swipe(xs, yTop, xs, yBot, swipeDuration);
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
                                                                            R.drawable.btr_next_ifm
                                                                    ) != 0)
                                                                    {
                                                                        break checkButtonAgainLoop;
                                                                    }

                                                                    // TẦNG 2: DÙNG MẮT THẦN THÔNG MINH (ASBL -> XML -> OCR)
                                                                    List<HSQTools.TextBlock> smartList = getCheckAnswerSmart();

                                                                    // 🌟 2.1: KIỂM TRA ĐẶC NHIỆM (ĐẠI ĐỘI NÚT TRỐNG)
                                                                    // Cứu cánh nếu Gemini bảo bấm ">" nhưng XML đéo có text
                                                                    if (isArrow || targetNorm.equals(">") || targetNorm.equals(">>") || targetNorm.equals("->")) {
                                                                        try {
                                                                            String xmlForArrow = HSQTools.getFlexibleXML();
                                                                            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlForArrow.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                            org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                            for (int i = 0; i < nodes.getLength(); i++) {
                                                                                org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);
                                                                                String clazz = node.getAttribute("class");
                                                                                String text = node.getAttribute("text");
                                                                                String desc = node.getAttribute("content-desc");

                                                                                // Nút nằm ở nửa dưới, là dạng Button/Image, đéo có chữ
                                                                                if ((clazz.contains("Button") || clazz.contains("ImageView") || clazz.contains("Image"))
                                                                                        && text.trim().isEmpty() && desc.trim().isEmpty()) {

                                                                                    android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                    if (r != null && r.centerY() > 1000) { // Nằm nửa dưới màn hình
                                                                                        updateNotificationContent("Tóm được NÚT ẢNH TRỐNG (Nghi ngờ là dấu >) tại " + r.centerX() + "," + r.centerY());
                                                                                        click(r.centerX(), r.centerY(), false);
                                                                                        break checkButtonAgainLoop;
                                                                                    }
                                                                                }
                                                                            }
                                                                        } catch (Exception ignored) {}
                                                                    }

                                                                    // 🌟 2.2: LƯỚI QUÉT TIÊU CHUẨN (CÓ CHỮ THÌ MỚI BẤM)
                                                                    HSQTools.TextBlock btnSmart = smartList.stream()
                                                                            .filter(x -> x.y > 180)
                                                                            .filter(x ->
                                                                            {
                                                                                // 🔥 BỌC THÉP 2: Đang tìm mũi tên thì lấy text gốc trên màn hình. Tìm chữ thì mới dọn dẹp.
                                                                                String clean = isArrow ? x.text.trim() : HSQTools.getOnlyTextLinq(normalizeText(x.text));

                                                                                // 🚫 LƯỚI LỌC TỬ HÌNH 1: Rỗng thì cút ngay lập tức!
                                                                                if (clean.isEmpty())
                                                                                    return false;

                                                                                // 🔥 ƯU TIÊN TUYỆT ĐỐI 1: Khớp chuẩn xác theo chỉ định của Gemini
                                                                                if (!targetNorm.isEmpty())
                                                                                {
                                                                                    // 1. Luôn an toàn tuyệt đối nếu khớp 100%
                                                                                    if (clean.equals(targetNorm))
                                                                                        return true;

                                                                                    // 2. 🚫 CHỐNG ẢO GIÁC "46-60": Chỉ cho phép chứa (contains) nếu chuỗi đích dài >= 2 ký tự
                                                                                    // (HOẶC nó là cái dấu lớn hơn ">" / "→" xịn xò)
                                                                                    boolean isSafeToContains = targetNorm.length() >= 2 || targetNorm.equals(">") || targetNorm.equals("→");
                                                                                    if (isSafeToContains && clean.contains(targetNorm))
                                                                                        return true;

                                                                                    // 3. Đối chiếu ngược cũng phải >= 3 ký tự cho chắc cú
                                                                                    if (clean.length() >= 3 && targetNorm.contains(clean))
                                                                                        return true;
                                                                                }

                                                                                // 🌟 ƯU TIÊN 2: Bộ từ khóa dự phòng cho các kịch bản cũ / Click mù
                                                                                return clean.matches("^(continue|next|submit|tieptuc|tieptheo|tieptheo>|done|gui|send|batdau|agree|accept|agreeandcontinue|>|>>|>>>|gotonextquestion|fwd|forward|tiep)$");
                                                                            })
                                                                            .max(Comparator.comparingInt((HSQTools.TextBlock x) -> x.y)) // Ưu tiên nút ở THẤP NHẤT
                                                                            .orElse(null);

                                                                    if (btnSmart != null)
                                                                    {
                                                                        updateNotificationContent("Tìm thấy nút chữ xịn: " + btnSmart.text + " tại (" + btnSmart.x + "," + btnSmart.y + ")");
                                                                        click(btnSmart.x, btnSmart.y, false);
                                                                        break checkButtonAgainLoop;
                                                                    }

                                                                    // TẦNG 3: TRUY QUÉT DỰ PHÒNG BẰNG XML (Chỉ tin nếu nằm trong vùng hiển thị an toàn)
                                                                    String currentXml = HSQTools.getFlexibleXML();
                                                                    android.graphics.Rect xmlBtnRect = FindNextButtonBoundsFromXmlString(currentXml);

                                                                    if (xmlBtnRect != null)
                                                                    {
                                                                        int btnHeight = xmlBtnRect.height();
                                                                        int btnWidth = xmlBtnRect.width();
                                                                        int centerY = xmlBtnRect.centerY();

                                                                        if (btnHeight > 20 && btnWidth > 20 && centerY > 180 && centerY < 2950)
                                                                        {
                                                                            updateNotificationContent("Tìm thấy nút XML xịn (Có thể là Sticky): " + xmlBtnRect.centerX() + "," + centerY);
                                                                            click(xmlBtnRect.centerX(), centerY, false);
                                                                            break checkButtonAgainLoop;
                                                                        }
                                                                        else if (btnHeight == 0 || btnWidth == 0)
                                                                        {
                                                                            updateNotificationContent("Phát hiện nút ma (Height/Width = 0), kệ mẹ nó để tụt xuống Tầng 4 vuốt màn!");
                                                                        }
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
                                                                                    "Mày bảo tao bấm nút [" + step + "] nhưng tao đã lật tung cả cái màn hình lên đéo thấy cái nút nào cả, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời"
                                                                            );
                                                                            continue stateMachine;
                                                                        }
                                                                    }

                                                                    tempCompare = new ArrayList<>(currentVisible);

                                                                    if (vuotLenLai == 0)
                                                                    {
                                                                        swipe(xs, yBot, xs, yTop, swipeDuration);
                                                                    }
                                                                    else
                                                                    {
                                                                        swipe(xs, yTop, xs, yBot, swipeDuration);
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
                                                                                "Mày bị ngáo à? Ô Quận/Huyện đéo được nhập tên Tỉnh (Hà Nội/HCM). Phải nhập tên 1 quận cụ thể! Hãy sửa lại giá trị nhập."
                                                                        );
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
                                                                                    if (r != null && r.top >= labelBottom - 50 && r.top <= labelBottom + 300)
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
                                                                            delay(2000);

                                                                            if (HSQTools.isKeyboardVisibleSmart())
                                                                            {
                                                                                clearAllText();
                                                                                delay(1000);
                                                                                inputText(valueToInput, null, false);
                                                                                delay(2500); // Chờ list load (nếu có)

                                                                                break checkInputSmartLoop; // Hoàn thành ô nhập thường, thoát!
                                                                            }
                                                                            else
                                                                            {
                                                                                handleActionFailure("Input_Keyboard", step, currentVisible, "Tao click vào ô nhập rồi nhưng bàn phím đéo lên!, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời");
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
                                                                                        "Lỗi Input: Tao đã cuộn nát cái form này từ đỉnh xuống đáy rồi ngược lại mà đéo thấy ô nhập [" + labelToFind + "] đâu cả!, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời"
                                                                                );
                                                                                continue stateMachine;
                                                                            }
                                                                        }

                                                                        // Lưu cấu trúc màn hình hiện tại để lượt sau so sánh kẹt
                                                                        tempCompare = new ArrayList<>(currentVisible);

                                                                        // Tiến hành vuốt dựa trên trạng thái "Quay xe" (vuotLenLai)
                                                                        if (vuotLenLai == 0)
                                                                        {
                                                                            swipe(xs, yBot, xs, yTop, swipeDuration); // Vuốt lên = Cuộn xuống
                                                                        }
                                                                        else
                                                                        {
                                                                            swipe(xs, yTop, xs, yBot, swipeDuration); // Vuốt xuống = Cuộn lên
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
                                                                    int vuotLenLai = 0;      // Theo dõi cuộn dọc tìm HÀNG
                                                                    int vuotNgangLai = 0;    // Theo dõi cuộn ngang tìm CỘT
                                                                    boolean swipeUp = false;

                                                                    checkMatrixActionLoop:
                                                                    while (true)
                                                                    {
                                                                        // 0. LẤY DATA MÀN HÌNH THÔNG MINH
                                                                        List<TextBlock> smartList = getCheckAnswerSmart();
                                                                        List<TextBlock> currentVisible = smartList.stream()
                                                                                .filter(x -> x.y > 180 && x.y < 2800).collect(Collectors.toList());

                                                                        // 1. TÌM TỌA ĐỘ HÀNG GỐC (ROW Y)
                                                                        android.graphics.Point rowPt = HSQTools.smartFindTextPoint(rowLabel);
                                                                        int targetY = (rowPt != null) ? rowPt.y : -1;

                                                                        // ⚡ ĐỘC CHIÊU CHỮ NHIỀU DÒNG: Dóng lại Y chuẩn từ tâm nút Radio thực tế
                                                                        int preciseY = targetY;
                                                                        List<android.graphics.Rect> rowElements = new ArrayList<>();

                                                                        if (targetY != -1)
                                                                        {
                                                                            String xmlForScan = HSQTools.getFlexibleXML();
                                                                            for (int offset = -40; offset <= 160; offset += 40)
                                                                            {
                                                                                rowElements = findMatrixElementsInRow(xmlForScan, targetY + offset);
                                                                                if (!rowElements.isEmpty())
                                                                                {
                                                                                    preciseY = rowElements.get(0).centerY(); // Chốt tâm Y nút xịn
                                                                                    break;
                                                                                }
                                                                            }
                                                                        }

                                                                        // 2. TÌM TỌA ĐỘ CỘT (COL X)
                                                                        int clickX = -1;
                                                                        if (targetY != -1)
                                                                        {
                                                                            boolean isNumericCol = colLabel.matches("-?\\d+");
                                                                            if (isNumericCol)
                                                                            {
                                                                                int targetColIndex = Integer.parseInt(colLabel) - 1;
                                                                                if (!rowElements.isEmpty() && targetColIndex < rowElements.size())
                                                                                {
                                                                                    clickX = rowElements.get(targetColIndex).centerX();
                                                                                }
                                                                            }
                                                                            else
                                                                            {
                                                                                // Kiểm tra bộ nhớ đệm cache trước
                                                                                if (matrixColumnCache.containsKey(colLabel))
                                                                                {
                                                                                    clickX = matrixColumnCache.get(colLabel);
                                                                                }
                                                                                else
                                                                                {
                                                                                    android.graphics.Point headerPt = HSQTools.smartFindTextPoint(colLabel);
                                                                                    if (headerPt != null)
                                                                                    {
                                                                                        clickX = headerPt.x;
                                                                                        matrixColumnCache.put(colLabel, headerPt.x); // Găm cache
                                                                                    }
                                                                                }

                                                                                // SMART SNAPPING X: Ép tọa độ X hít vào tâm ô tròn Radio gần nhất trên hàng
                                                                                if (clickX != -1 && !rowElements.isEmpty())
                                                                                {
                                                                                    final int finalClickX = clickX;
                                                                                    android.graphics.Rect closestBox = rowElements.stream()
                                                                                            .min(Comparator.comparingInt(r -> Math.abs(r.centerX() - finalClickX)))
                                                                                            .orElse(null);
                                                                                    if (closestBox != null)
                                                                                    {
                                                                                        clickX = closestBox.centerX();
                                                                                    }
                                                                                }
                                                                            }
                                                                        }

                                                                        // ========================================================
                                                                        // ⚡ TRẠM ĐIỀU HƯỚNG SAI SỐ HAI TRỤC (DỌC & NGANG)
                                                                        // ========================================================

                                                                        // TRƯỜNG HỢP A: KHÔNG TÌM THẤY HÀNG (ROW Y = -1) -> VUỐT DỌC XUỐNG DƯỚI
                                                                        if (targetY == -1)
                                                                        {
                                                                            if (vuotLenLai < 3)
                                                                            {
                                                                                tempCompare = currentVisible;
                                                                                updateNotificationContent("Không thấy hàng, đang vuốt dọc xuống dưới...");
                                                                                swipe(720, 2400, 720, 1200, 1500);
                                                                                delay(2000);
                                                                                vuotLenLai++;
                                                                                continue checkMatrixActionLoop;
                                                                            }
                                                                            handleActionFailure("Matrix_Row", step, currentVisible, "Lỗi Matrix: Không tìm thấy hàng chữ [" + rowLabel + "]!, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời");
                                                                            continue stateMachine;
                                                                        }

                                                                        // TRƯỜNG HỢP B: THẤY HÀNG NHƯNG MÙ CỘT (CLICK X = -1) -> VUỐT NGANG SANG PHẢI
                                                                        if (clickX == -1)
                                                                        {
                                                                            if (vuotNgangLai < 3)
                                                                            {
                                                                                updateNotificationContent("Thiếu cột dữ liệu, đang cuộn bảng sang bên phải...");
                                                                                // Vuốt từ lề phải (1320) qua lề trái (120) tại chính dòng preciseY để cuộn các cột ẩn ra
                                                                                swipe(1320, preciseY, 120, preciseY, 1200);
                                                                                delay(2500);
                                                                                vuotNgangLai++;
                                                                                continue checkMatrixActionLoop;
                                                                            }

                                                                            // Biện pháp giải cứu cuối cùng: Vuốt trả ngược kịch biên về lề trái trước khi báo tử
                                                                            swipe(120, preciseY, 1320, preciseY, 1200);
                                                                            delay(2000);
                                                                            handleActionFailure("Matrix_Col", step, currentVisible, "Lỗi Matrix: Đã lật bảng sang phải nhưng không thấy cột [" + colLabel + "]!, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời");
                                                                            continue stateMachine;
                                                                        }

                                                                        // 3. THỰC THI DI CHUYỂN KHUNG HÀNG NẾU QUÁ THẤP
                                                                        if (rowPt != null && rowPt.y > 2300 && !swipeUp)
                                                                        {
                                                                            swipe(720, 2200, 720, 1500, 1500);
                                                                            delay(2000);
                                                                            swipeUp = true;
                                                                            continue checkMatrixActionLoop;
                                                                        }

                                                                        // 4. CHỌT CHÍNH XÁC VÀO TIM ĐEN
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
                                                                break checkMatrixSmartLoop;
                                                            }
                                                        }
                                                        else if (step.contains("dragdrop"))
                                                        {
                                                            while (true)
                                                            {
                                                                Matcher match = Pattern.compile("\\{([^}]+)\\}").matcher(step);
                                                                if (match.find() && match.group(1).contains("~"))
                                                                {
                                                                    String[] parts = match.group(1).split("~");
                                                                    if (parts.length == 2)
                                                                    {
                                                                        String sourceStr = parts[0];
                                                                        String targetStr = parts[1];

                                                                        updateNotificationContent("Smart DragDrop: " + sourceStr + " -> " + targetStr);
                                                                        List<TextBlock> tempCompare = new ArrayList<>();
                                                                        int vuotLenLai = 0;

                                                                        while (true)
                                                                        {
                                                                            // 1. DÙNG MẮT THẦN HYBRID TÌM TỌA ĐỘ 2 ĐIỂM
                                                                            android.graphics.Point sourcePt = HSQTools.smartFindTextPoint(sourceStr);
                                                                            android.graphics.Point targetPt = HSQTools.smartFindTextPoint(targetStr);

                                                                            // 2. NẾU THẤY CẢ 2 -> THỰC THI KÉO THẢ
                                                                            if (sourcePt != null && targetPt != null)
                                                                            {
                                                                                updateNotificationContent("Đang kéo thả...");
                                                                                // Kéo từ tâm source đến tâm target (thêm 300px bù trừ theo logic cũ của sếp nếu cần)
                                                                                swipe(sourcePt.x, sourcePt.y, targetPt.x, targetPt.y + 100, 1500);
                                                                                delay(3000);
                                                                                break; // Thành công
                                                                            }

                                                                            // 3. KIỂM TRA LỖI & BÁO TELEGRAM VIP
                                                                            List<TextBlock> currentScreen = getCheckAnswerSmart();
                                                                            List<TextBlock> currentVisible = currentScreen.stream().filter(x -> x.y > 180).collect(Collectors.toList());

                                                                            if (sourcePt == null || targetPt == null || HSQTools.areAlmostSame(tempCompare, currentVisible, 20))
                                                                            {
                                                                                // Thử vuốt tìm 2 lần trước khi báo cáo
                                                                                if (vuotLenLai < 2)
                                                                                {
                                                                                    tempCompare = currentVisible;
                                                                                    updateNotificationContent("Không thấy đủ 2 điểm, đang vuốt tìm...");
                                                                                    swipe(720, 2600, 720, 1000, 1500);
                                                                                    delay(2000);
                                                                                    vuotLenLai++;
                                                                                    continue;
                                                                                }

                                                                                // THỰC SỰ LỖI: GỬI TELEGRAM VIP
                                                                                handleActionFailure(
                                                                                        "DragDrop", step, getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList()),
                                                                                        "Lỗi kéo thả: Tao không thấy đủ 2 điểm [" + sourceStr + "] và [" + targetStr + "], kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời"
                                                                                );
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
                                                            checkDropdownSmartLoop:
                                                            while (true)
                                                            {
                                                                Matcher match = Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                                if (match.find())
                                                                {
                                                                    String contextStr = match.group(1);
                                                                    updateNotificationContent("Smart Dropdown: " + contextStr);

                                                                    List<TextBlock> currentScreen = getCheckAnswerSmart();
                                                                    int vuotLenLai = 0;

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
                                                                                    return cleanText != null && (cleanText.contains(normTarget) || normTarget.contains(cleanText));
                                                                                })
                                                                                // Sắp xếp: Thằng nào bằng tuyệt đối lên đầu, rồi đến thằng nào có độ dài NGẮN NHẤT
                                                                                // Để loại bỏ các câu hỏi dài dằng dặc chứa từ khóa trùng lặp
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
                                                                                swipe(720, 2600, 720, 1000, 1500);
                                                                                delay(2000);
                                                                                currentScreen = getCheckAnswerSmart();
                                                                                vuotLenLai++;
                                                                                continue checkDropdownActionLoop;
                                                                            }
                                                                            handleActionFailure(
                                                                                    "Dropdown", step, currentScreen,
                                                                                    "Mày đưa lệnh [" + step + "] nhưng tao cuộn nát máy không thấy chữ này đâu, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời"
                                                                            );
                                                                            continue stateMachine;
                                                                        }

                                                                        // DANH SÁCH ỨNG VIÊN CLICK (THEO THỨ TỰ ƯU TIÊN GIẢM DẦN)
                                                                        List<android.graphics.Point> candidates = new ArrayList<>();

                                                                        // =========================================================
                                                                        // 🎯 1. ƯU TIÊN TỐI CAO: THUẬT TOÁN "NODE SONG SINH" (TWIN NODE)
                                                                        // =========================================================
                                                                        List<HSQTools.TextBlock> twinNodes = currentScreen.stream()
                                                                                .filter(x -> x.y > 180)
                                                                                .filter(x ->
                                                                                {
                                                                                    String cleanText = HSQTools.getOnlyTextLinq(normalizeText(x.text));
                                                                                    return !cleanText.isEmpty() && (cleanText.contains(normTarget) || normTarget.contains(cleanText));
                                                                                })
                                                                                .collect(Collectors.toList());

                                                                        if (twinNodes.size() >= 2)
                                                                        {
                                                                            twinNodes.sort((a, b) -> Integer.compare(b.y, a.y)); // Lấy Y thấp nhất (to nhất) lên đầu
                                                                            HSQTools.TextBlock dropdownNode = twinNodes.get(0);
                                                                            HSQTools.TextBlock questionNode = twinNodes.get(twinNodes.size() - 1);

                                                                            if (dropdownNode.y > questionNode.y + 50)
                                                                            {
                                                                                updateNotificationContent("Twin Node: Bắt được Dropdown ẩn tại Y=" + dropdownNode.y);
                                                                                candidates.add(new android.graphics.Point(dropdownNode.x, dropdownNode.y));
                                                                            }
                                                                        }

                                                                        // =========================================================
                                                                        // 🎯 2. THUẬT TOÁN "NODE LIỀN KỀ THÔNG MINH" (Bỏ qua Node Rác)
                                                                        // =========================================================
                                                                        if (candidates.isEmpty())
                                                                        {
                                                                            String currentXml = HSQTools.getFlexibleXML();
                                                                            try
                                                                            {
                                                                                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                                                                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                                                                                org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(currentXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                                                                org.w3c.dom.NodeList nodes = doc.getElementsByTagName("node");

                                                                                boolean foundAnchor = false;
                                                                                int anchorBottomY = exactTextNode.y;

                                                                                for (int i = 0; i < nodes.getLength(); i++)
                                                                                {
                                                                                    org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(i);

                                                                                    if (!foundAnchor)
                                                                                    {
                                                                                        String text = node.getAttribute("text");
                                                                                        String desc = node.getAttribute("content-desc");
                                                                                        String combined = HSQTools.getOnlyTextLinq(normalizeText(text + " " + desc));

                                                                                        if (!combined.isEmpty() && (combined.contains(normTarget) || normTarget.contains(combined)))
                                                                                        {
                                                                                            foundAnchor = true;
                                                                                            android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));
                                                                                            if (r != null)
                                                                                                anchorBottomY = r.bottom;
                                                                                        }
                                                                                    }
                                                                                    else
                                                                                    {
                                                                                        android.graphics.Rect r = HSQTools.parseBoundsFromXml(node.getAttribute("bounds"));

                                                                                        // Lấy tất cả các node nằm dưới câu hỏi, khoảng cách không quá xa (để né nút Next)
                                                                                        if (r != null && r.top >= anchorBottomY - 20 && r.top < anchorBottomY + 400 && r.height() > 40)
                                                                                        {

                                                                                            String text = node.getAttribute("text").toLowerCase();
                                                                                            String desc = node.getAttribute("content-desc").toLowerCase();
                                                                                            String combined = text + " " + desc;
                                                                                            String cleanCombined = HSQTools.getOnlyTextLinq(normalizeText(combined));

                                                                                            // Lưới lọc rác: Nếu đụng phải câu chửi/hướng dẫn -> Mặc kệ nó, CONTINUE vòng lặp để đi xuống tiếp!
                                                                                            if (combined.contains("đáp án") || combined.contains("answer") ||
                                                                                                    combined.contains("câu trả lời") || combined.contains("vui lòng") || cleanCombined.length() > 20)
                                                                                            {
                                                                                                continue;
                                                                                            }

                                                                                            // BỘ LỌC NHẬN DIỆN DROPDOWN
                                                                                            boolean hasTextMatch = combined.contains("chọn một") || combined.contains("select") || combined.contains("choose");

                                                                                            // 🔥 FIX 1: Ép width < 1000 để vứt bỏ cái Node Cha tàng hình bọc ngoài cùng!
                                                                                            boolean isEmptyContainer = combined.trim().isEmpty() && r.height() > 60 && r.width() > 200 && r.width() < 1000;
                                                                                            boolean isClickableView = node.getAttribute("clickable").equals("true") || !node.getAttribute("class").contains("TextView");

                                                                                            boolean isDropdownLike = hasTextMatch || (isEmptyContainer && isClickableView);

                                                                                            if (isDropdownLike)
                                                                                            {
                                                                                                // 🔥 FIX 2: CHỐNG CHỌT HỤT TRỤC X
                                                                                                // Nếu cái node vô tình quá rộng (> 800), không chọt giữa 720 nữa mà chọt lệch sang trái (cách lề 200px)
                                                                                                int safeX = r.width() > 800 ? r.left + 200 : r.centerX();

                                                                                                updateNotificationContent("Bắt được Node Sibling xịn tại X=" + safeX + ", Y=" + r.centerY());
                                                                                                candidates.add(new android.graphics.Point(safeX, r.centerY()));
                                                                                                break; // Bắt được hàng xịn mới chốt và thoát vòng lặp!
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                            catch (Exception ignored)
                                                                            {
                                                                            }
                                                                        }

                                                                        // =========================================================
                                                                        // 🎯 3. FALLBACK ASBL (Tìm vùng quanh Anchor bằng Service)
                                                                        // =========================================================
                                                                        if (candidates.isEmpty())
                                                                        {
                                                                            AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
                                                                            if (root != null)
                                                                            {
                                                                                android.graphics.Point asblPt = findDropdownNearByASBL(root, exactTextNode.x, exactTextNode.y, contextStr);
                                                                                if (asblPt != null)
                                                                                    candidates.add(asblPt);
                                                                                root.recycle();
                                                                            }
                                                                        }

                                                                        // =========================================================
                                                                        // 🎯 4. FALLBACK HÌNH HỌC OCR (BẮT MŨI TÊN/CHỮ "CHỌN" BÊN DƯỚI)
                                                                        // =========================================================
                                                                        if (candidates.isEmpty())
                                                                        {
                                                                            HSQTools.TextBlock dropdownSign = currentScreen.stream()
                                                                                    .filter(n -> n.y > exactTextNode.y + 80 && n.y < exactTextNode.y + 800)
                                                                                    .filter(n ->
                                                                                    {
                                                                                        String c = HSQTools.getOnlyTextLinq(normalizeText(n.text));
                                                                                        // 🌟 Bổ sung thêm "chonmot" vào vì hàm normalizeText sẽ dính các chữ vào nhau
                                                                                        return c.equals("v") || c.equals("chon") || c.equals("select") || c.equals("choose") || c.equals("chonmot") || c.equals("selectone");
                                                                                    })
                                                                                    .max(Comparator.comparingInt((HSQTools.TextBlock n) -> n.x))
                                                                                    .orElse(null);

                                                                            if (dropdownSign != null)
                                                                            {
                                                                                updateNotificationContent("Quét thấy mũi tên/chữ Chọn: " + dropdownSign.text);
                                                                                candidates.add(new android.graphics.Point(dropdownSign.x, dropdownSign.y));
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
                                                                            handleActionFailure(
                                                                                    "Dropdown", step, currentScreen,
                                                                                    "Tao đã thử chọt hết các ứng viên của [" + step + "] nhưng màn hình không đổi, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời"
                                                                            );
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
                                                                                "Mày bảo chọn ô thứ " + targetIndex + " nhưng tao đếm nát cả màn hình chỉ thấy " + currentGlobalCount + " ô. Đếm lại đi con trai!"
                                                                        );
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
                                                                                        swipe(720, 2600, 720, 1000, 1500);
                                                                                        delay(2000);
                                                                                        vuotLenLaiHeader++;
                                                                                        continue;
                                                                                    }

                                                                                    // THỰC SỰ LỖI: GỬI TELEGRAM VIP
                                                                                    handleActionFailure(
                                                                                            "Accordion", step, getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList()),
                                                                                            "Tao không thấy tiêu đề Accordion [" + headerStr + "]. Mày check lại xem nó có bị viết sai không?, kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời"
                                                                                    );
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
                                                                int tryAgain = 0;
                                                                Matcher match = java.util.regex.Pattern.compile("\\{([^{}]+)\\}").matcher(step);
                                                                if (match.find() && match.group(1).contains("~"))
                                                                {
                                                                    String[] parts = match.group(1).split("~");
                                                                    String headerStr = parts[0];
                                                                    String answerStr = parts[1];

                                                                    updateNotificationContent("Smart Block: " + headerStr + " -> " + answerStr);
                                                                    List<HSQTools.TextBlock> tempCompare = new ArrayList<>();
                                                                    int vuotLenLai = 0;

                                                                    checkBlockActionLoop:
                                                                    while (true)
                                                                    {
                                                                        // 0. LẤY DỮ LIỆU MÀN HÌNH THÔNG MINH (ASBL -> XML -> OCR)
                                                                        List<HSQTools.TextBlock> smartList = getCheckAnswerSmart();
                                                                        List<HSQTools.TextBlock> currentVisible = smartList.stream()
                                                                                .filter(x -> x.y > 180 && x.y < 2800).collect(Collectors.toList());

                                                                        // 1. TÌM TIÊU ĐỀ CÂU HỎI (HEADER)
                                                                        android.graphics.Point headerPt = HSQTools.smartFindTextPoint(headerStr);
                                                                        int targetY = (headerPt != null) ? headerPt.y : -1;

                                                                        // 2. XỬ LÝ NẾU HEADER NẰM QUÁ THẤP (ĐƯA RA GIỮA MÀN)
                                                                        if (targetY > 2300)
                                                                        {
                                                                            swipe(720, 2200, 720, 1500, 1000);
                                                                            delay(2000);
                                                                            continue checkBlockActionLoop;
                                                                        }

                                                                        android.graphics.Point finalClickPt = null;

                                                                        // 3. TÌM ĐÁP ÁN (SỐ HOẶC CHỮ)
                                                                        if (targetY != -1)
                                                                        {
                                                                            boolean isNumeric = answerStr.matches("\\d+");
                                                                            if (isNumeric)
                                                                            {
                                                                                // CHIẾN THUẬT THANG ĐIỂM: Tìm các nút bấm cùng hàng bên dưới câu hỏi
                                                                                String xml = HSQTools.getFlexibleXML();
                                                                                // Dóng hàng Y bên dưới Header khoảng 100-500px để tìm hàng nút số
                                                                                java.util.List<android.graphics.Rect> scaleElements = new ArrayList<>();
                                                                                for (int offset = 100; offset <= 500; offset += 100)
                                                                                {
                                                                                    scaleElements = findMatrixElementsInRow(xml, targetY + offset);
                                                                                    if (!scaleElements.isEmpty())
                                                                                        break;
                                                                                }

                                                                                int targetIndex = Integer.parseInt(answerStr) - 1;
                                                                                if (!scaleElements.isEmpty())
                                                                                {
                                                                                    if (targetIndex >= 0 && targetIndex < scaleElements.size())
                                                                                    {
                                                                                        finalClickPt = new android.graphics.Point(scaleElements.get(targetIndex).centerX(), scaleElements.get(targetIndex).centerY());
                                                                                    }
                                                                                    else
                                                                                    {
                                                                                        // Fallback: Click ô cuối cùng nếu index vượt quá
                                                                                        finalClickPt = new android.graphics.Point(scaleElements.get(scaleElements.size() - 1).centerX(), scaleElements.get(scaleElements.size() - 1).centerY());
                                                                                    }
                                                                                }
                                                                            }
                                                                            else
                                                                            {
                                                                                // CHIẾN THUẬT CHỮ: Tìm Text đáp án trong vùng lân cận Header
                                                                                String cleanAns = normalizeText(answerStr).replaceAll("[^a-z0-9]", "");
                                                                                HSQTools.TextBlock ansNode = currentVisible.stream()
                                                                                        .filter(x -> x.y > targetY + 20 && x.y < targetY + 800)
                                                                                        .filter(x ->
                                                                                        {
                                                                                            String nodeTxt = normalizeText(x.text).replaceAll("[^a-z0-9]", "");
                                                                                            return nodeTxt.equals(cleanAns) || nodeTxt.contains(cleanAns) || HSQTools.levenshtein(nodeTxt, cleanAns) <= (cleanAns.length() * 0.2);
                                                                                        })
                                                                                        .min(Comparator.comparingInt(x -> Math.abs(x.y - targetY)))
                                                                                        .orElse(null);
                                                                                if (ansNode != null)
                                                                                    finalClickPt = new android.graphics.Point(ansNode.x, ansNode.y);
                                                                            }
                                                                        }

                                                                        // --- CHỐT CHẶN KIỂM TRA LỖI & BÁO TELEGRAM VIP ---
                                                                        if (finalClickPt == null || HSQTools.areAlmostSame(tempCompare, currentVisible, 20))
                                                                        {
                                                                            if (vuotLenLai < 2 && finalClickPt == null)
                                                                            {
                                                                                tempCompare = currentVisible;
                                                                                updateNotificationContent("Không thấy Block, đang vuốt tìm...");
                                                                                swipe(720, 2600, 720, 1000, 1500);
                                                                                delay(2000);
                                                                                vuotLenLai++;
                                                                                continue checkBlockActionLoop;
                                                                            }

                                                                            // THỰC SỰ LỖI: GỬI BÁO CÁO VIP
                                                                            handleActionFailure(
                                                                                    "Block", step, getCheckAnswerSmart().stream().filter(x -> x.y > 180).collect(Collectors.toList()),
                                                                                    "Lỗi Block: Tao không thấy đáp án [" + answerStr + "] trong vùng của câu hỏi [" + headerStr + "], kiểm tra lại xem có đúng rule không và trả lời lại theo rule cho tao nhé, không cần diễn giải dài dòng, chỉ cần tập trung trả lời"
                                                                            );
                                                                            continue stateMachine;
                                                                        }

                                                                        // 4. THỰC THI CLICK
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
        String controlall;
        int i = 0;
        while (true)//load control từ server
        {
            i++;
            updateNotificationContent("Load control " + i);
            JSONObject controlserver = HSQHttps.getServerHttpheader("apiKey", apiKeyServer, "http://api.quaykute.id.vn/api/public/clone/2", JSONObject.class, false);
            try
            {
                if (controlserver != null)
                {
                    controlall = (controlserver.getJSONObject("resultObj")).getString("info");
                    String[] control = controlall.split(Pattern.quote("|"), -1);
                    apkVersion = Integer.parseInt(control[0]);
                    localServerIp = control[1];
                    if (!upDateTool())
                    {
                        continue;
                    }
                    idTelegram = control[2];
                    remotePromtVersion = Integer.parseInt(control[3]);
                    if (!updatePromt())
                    {
                        continue; // Nếu có lệnh update mà tải lỗi thì bắt vòng lặp load lại
                    }
                    break;
                }
            }
            catch (Exception ignored)
            {
            }
            HSQTools.delay(5000);
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
                String linkDownLoad;
                String filePath = "/sdcard/Download/SHTools.apk";
                if (HSQHttps.isServerReachable("http://" + localServerIp + ":5000"))
                {
                    linkDownLoad = "http://" + localServerIp + ":5000/download/apk/SHTools" + apkVersion + ".apk";
                }
                else
                {
                    linkDownLoad = "http://quay.hopto.org:5000/download/apk/SHTools" + apkVersion + ".apk";
                }
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
                            while(true)
                            {
                                int checkInstall = ASBLBridgeService.findMultiTextDesWindow(60, true, true, true, false, "install", "there was a problem parsing the package");
                                if (checkInstall == 2 || checkInstall == 0)
                                {
                                    updateContent("Lỗi cài apk " + tryReinstall);
                                    if(tryReinstall < 3) {
                                        delay(5000);
                                        tryReinstall++;
                                        continue;
                                    }
                                    delay(180000);
                                    continue beginInstall;
                                }
                                else {
                                    break;
                                }
                            }
                            delay(3000);
                            ASBLBridgeService.findMultiTextDesWindow(360, true, true, true, false, "decline" );
                            delay(3600000);
                            return true;
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
                                while(true)
                                {
                                    int checkInstall = ASBLBridgeService.findMultiTextDesWindow(60, true, true, true, false, "install", "there was a problem parsing the package");
                                    if (checkInstall == 2 || checkInstall == 0)
                                    {
                                        updateContent("Lỗi cài apk " + tryReinstall);
                                        if(tryReinstall < 3) {
                                            delay(5000);
                                            tryReinstall++;
                                            continue;
                                        }
                                        delay(180000);
                                        continue beginInstall;
                                    }
                                    else {
                                        break;
                                    }
                                }
                                delay(3000);
                                ASBLBridgeService.findMultiTextDesWindow(180, true, true, true, false, "decline");
                                delay(3600000);
                                return true;
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
                String linkDownLoad;
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
                return false;
            }
        }
        return true; // Nếu bằng version nhau thì phớt lờ, coi như đã up-to-date
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
        boolean isSuccess;
        int i = 0;
        while (true)
        {
            i++;
            updateNotificationContent("get " + Display + " " + i);
            JSONObject body = HSQHttps.getServerHttpheader("apiKey", api_key, "http://api.quaykute.id.vn/api/public/clone/" + type, JSONObject.class, false);
            if (body != null)
            {
                try
                {
                    isSuccess = body.getBoolean("isSuccessed");
                    if (isSuccess)
                    {
                        if (body.getString("resultObj").equals("null"))
                        {
                            show();
                            updateContent("Hết " + Display);
                            delay(60000);
                        }
                        else
                        {
                            try
                            {
                                String clonetest = (body.getJSONObject("resultObj")).getString("info");
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
                String getGemKey = getCloneFromServer(apiKeyGem, "Key Gem", 3, false, 999);
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
                    swipe(720, 2600, 720, 1000, 1500);
                    HSQTools.delay(2000);

                    String fileName = (i + 1 < 10) ? "/screenCapa" + (i + 1) + ".png" : "/screenCapb" + (i - 8) + ".png";
                    HSQTools.cropAndSaveScreen(fullScreen, imagePath + fileName);
                    HSQTools.delay(1000);
                }
                for (int i = 0; i < imageCount; i++)
                {
                    swipe(720, 1200, 720, 2800, 1500);
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
                    apiGemini = getNewApiGeminiKey();
                    geminiAI.setApiKey(apiGemini);
                    geminiAI.deleteChat(); // Dọn dẹp não cũ
                    createNewChatGemByApi(customAgentRule, false);
                    delay(5000);
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

        geminiAI.saveTurnToHistory(prompt, textAnswer);
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
                    apiGemini = getNewApiGeminiKey();
                    geminiAI.setApiKey(apiGemini);
                    geminiAI.deleteChat(); // Dọn dẹp não cũ
                    createNewChatGemByApi(customAgentRule, false);
                    delay(5000);
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
     * HÀM SIÊU CẤP: Lấy text từ mọi nguồn (ASBL -> XML -> OCR)
     * Trả về List TextBlock chuẩn để logic so sánh 3 tầng thực thi.
     */
    private List<HSQTools.TextBlock> getCheckAnswerSmart()
    {
        List<HSQTools.TextBlock> finalGrid = new ArrayList<>();

        AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
        if (root != null)
        {
            root.refresh();
            java.util.List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
            getAllNodesRec(root, allNodes);

            for (AccessibilityNodeInfo node : allNodes)
            {
                // 🌟 LẤY TEXT HOẶC LẤY CONTENT-DESC NẾU TEXT RỖNG
                CharSequence nodeText = node.getText();
                if (nodeText == null || nodeText.toString().trim().isEmpty())
                {
                    nodeText = node.getContentDescription();
                }

                if (nodeText != null && !nodeText.toString().trim().isEmpty())
                {
                    android.graphics.Rect bounds = new android.graphics.Rect();
                    node.getBoundsInScreen(bounds);

                    int cx = bounds.centerX();
                    int cy = bounds.centerY();

                    if (cy > 180 && cy < (heightOfScreen - 150) && bounds.width() > 5 && bounds.height() > 5)
                    {
                        finalGrid.add(new HSQTools.TextBlock(nodeText.toString(), cx, cy));
                    }
                }
            }
            root.recycle();
        }

        if (finalGrid.size() < 5)
        {
            String xml = getFlexibleXML();
            finalGrid = convertXmlToTextBlocks(xml);
        }

        if (finalGrid.size() < 3)
        {
            finalGrid = getOcrTextBlocks();
        }

        return finalGrid;
    }

    /**
     * Hàm bổ trợ biến OCR của ML Kit thành List TextBlock có tọa độ để click
     */
    private List<HSQTools.TextBlock> getOcrTextBlocks()
    {
        List<HSQTools.TextBlock> list = new ArrayList<>();
        Bitmap screenshot = HSQTools.getScreenBitmap();
        if (screenshot == null) return list;

        try
        {
            InputImage image = InputImage.fromBitmap(screenshot, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            com.google.mlkit.vision.text.Text visionText = Tasks.await(recognizer.process(image));

            for (com.google.mlkit.vision.text.Text.TextBlock block : visionText.getTextBlocks())
            {
                for (com.google.mlkit.vision.text.Text.Line line : block.getLines())
                {
                    android.graphics.Rect r = line.getBoundingBox();
                    if (r != null)
                    {
                        list.add(new HSQTools.TextBlock(line.getText(), r.centerX(), r.centerY()));
                    }
                }
            }
        }
        catch (Exception e)
        {
            delay(1000);
        }
        finally
        {
            screenshot.recycle();
        }
        return list;
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
    private void handleActionFailure(String actionType, String stepDetail, List<HSQTools.TextBlock> currentVisible, String aiPrompt)
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
                textAnswer = getAnswerFromGemByApi(lastImageCount, false, false, aiPrompt);
                currentState = STATE_ANSWER_OK;
            }
        }
        else
        {
            AINguL++;
            screenBegin = new ArrayList<>(currentVisible);
            // Gửi tham số aiPrompt tùy biến để "vả" AI
            textAnswer = getAnswerFromGemByApi(lastImageCount, false, false, aiPrompt);
            currentState = STATE_ANSWER_OK;
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

}


