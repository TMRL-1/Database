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

    public static void main(String[] args) throws IOException {
        Pattern tagsPattern = Pattern.compile("^((?!\\|)[0-9a-zA-Z\\u0000-\\u00FF])+$");
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(5, TimeUnit.MINUTES).readTimeout(10, TimeUnit.MINUTES).build();
        System.out.println("获取Tag数据");
        Request request = new Request.Builder().url("https://yande.re/tag.json?limit=0&order=name").build();
        Response response = client.newCall(request).execute();
        String jsonTags = response.body().string();
        System.out.println("解析Tag数据");
        Type type = new TypeToken<List<TagBean>>() {
        }.getType();
        List<TagBean> tags = gson.fromJson(jsonTags, type);
        System.out.println("获取翻译数据");
        request = new Request.Builder().url("https://docs.google.com/document/d/e/2PACX-1vQSeSI4PU70eBoonfjiNpzQY66VqpwZoJDW4fUS8piXTTpQe57LKDb1KRZnRZEOtwH9Cvhq1dW2hLVp/pub").build();
        response = client.newCall(request).execute();
        String translateText = StringEscapeUtils.unescapeHtml4(response.body().string());
//        System.out.println(translateText);
        Pattern translatePattern = Pattern.compile("<p class=\"c1\"><span class=\"c0\">(.+?)\\|(.+?)<\\/span><\\/p>");
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
        for (TagBean tag : tags) {
            Matcher matcher = tagsPattern.matcher(tag.getName());
            if (matcher.find()) {
                if (translateMap.containsKey(tag.getName())) {
                    Files.write(Paths.get("./tags/" + Base64.getUrlEncoder().encodeToString(tag.getName().getBytes(StandardCharsets.UTF_8))), translateMap.get(tag.getName()).getBytes(StandardCharsets.UTF_8));
                } else {
                    Files.write(Paths.get("./tags/" + Base64.getUrlEncoder().encodeToString(tag.getName().getBytes(StandardCharsets.UTF_8))), tag.getName().getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        System.out.println("数据写入完成");
    }
}

class TagBean {
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
