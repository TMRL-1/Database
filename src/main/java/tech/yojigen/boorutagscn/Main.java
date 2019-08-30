package tech.yojigen.boorutagscn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Main {
    private static final String TEMPLATE_JS_DANMAKU = "word_arr.push({x:random(0,width),y:random(0,height),text:'%s',size:random(txt_min_size,txt_max_size)});\n";
    private static final String URL_TAGS = "https://yande.re/tag.json?order=count&limit=110";
    private static final String URL_EXCEL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vSTUlGa0rPVJ0KQ9a0EIGszpOQQSRI-DhRC21Uypl5nW-t22fAaJ4GyAfkjjeoz1XJ6ECMnZndH_UZo/pubhtml";

    public static void main(String[] args) throws IOException {
        String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36";
        Pattern tagsPattern = Pattern.compile("^((?!\\|)[0-9a-zA-Z\\u0000-\\u00FF])+$");
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(5, TimeUnit.MINUTES).readTimeout(10, TimeUnit.MINUTES).build();
        System.out.println("获取Tag数据");
        Request request = new Request.Builder().url(URL_TAGS).removeHeader("User-Agent").addHeader("User-Agent", USER_AGENT).build();
        Response response = client.newCall(request).execute();
        String jsonTags = response.body().string();
        System.out.println("解析Tag数据");
        Type type = new TypeToken<List<TagBean>>() {
        }.getType();
        List<TagBean> tags = gson.fromJson(jsonTags, type);
        System.out.println("获取翻译数据");
        request = new Request.Builder().url(URL_EXCEL).removeHeader("User-Agent").addHeader("User-Agent", USER_AGENT).build();
        response = client.newCall(request).execute();
        String translateText = StringEscapeUtils.unescapeHtml4(response.body().string());
//        System.out.println(translateText);
        Pattern translatePattern = Pattern.compile("<td class=\"s0\">([0-9a-zA-Z\\u0000-\\u00FF]+?)<\\/td><td class=\"s1\">(.+?)<\\/td>");
        Matcher translateMatcher = translatePattern.matcher(translateText);
        System.out.println("解析翻译数据");
        Map<String, String> translateMap = new HashMap<>();
        while (translateMatcher.find()) {
            translateMap.put(translateMatcher.group(1), translateMatcher.group(2) + "[" + translateMatcher.group(1) + "]");
        }
        if (!Files.exists(Paths.get("./tags"), LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectory(Paths.get("./tags"));
        }
        System.out.println("写入翻译数据");
//        for (String key : translateMap.keySet()) {
//            System.out.println(key + "|" + translateMap.get(key));
//        }
        StringBuilder stringBuilder = new StringBuilder();
        for (TagBean tag : tags) {
            Matcher matcher = tagsPattern.matcher(tag.getName());
            if (matcher.find()) {
                if (translateMap.containsKey(tag.getName())) {
                    Files.write(Paths.get("./tags/" + Base64.getUrlEncoder().encodeToString(tag.getName().getBytes(StandardCharsets.UTF_8))), translateMap.get(tag.getName()).getBytes(StandardCharsets.UTF_8));
//                    System.out.println("写入翻译" + translateMap.get(tag.getName()));
                    stringBuilder.append(String.format(TEMPLATE_JS_DANMAKU, translateMap.get(tag.getName())));
                } else {
                    Files.write(Paths.get("./tags/" + Base64.getUrlEncoder().encodeToString(tag.getName().getBytes(StandardCharsets.UTF_8))), tag.getName().getBytes(StandardCharsets.UTF_8));
//                    System.out.println("写入原文" + tag.getName());
                }
            }
        }
        Files.write(Paths.get("./index/js/tags.js"), stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("数据写入完成");
    }
}

class TagBean {
    private String name;

    String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
