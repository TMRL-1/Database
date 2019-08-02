package tech.yojigen.boorutagscn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
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
        Pattern pattern = Pattern.compile("^((?!\\|)[0-9a-zA-Z\\u0000-\\u00FF])+$");
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(5, TimeUnit.MINUTES).readTimeout(10, TimeUnit.MINUTES).build();
        System.out.println("获取数据");
        Request request = new Request.Builder().url("https://yande.re/tag.json?limit=0&order=name").build();
        Response response = client.newCall(request).execute();
        String jsonTags = response.body().string();
        System.out.println("解析数据");
        Type type = new TypeToken<List<TagBean>>() {
        }.getType();
        List<TagBean> tags = gson.fromJson(jsonTags, type);
        if (!Files.exists(Paths.get("./tags"), LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectory(Paths.get("./tags"));
        }
        for (TagBean tag : tags) {
            Matcher matcher = pattern.matcher(tag.getName());
            if (matcher.find()) {
                Files.write(Paths.get("./tags/" + Base64.getUrlEncoder().encodeToString(tag.getName().getBytes(StandardCharsets.UTF_8))), tag.getName().getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}