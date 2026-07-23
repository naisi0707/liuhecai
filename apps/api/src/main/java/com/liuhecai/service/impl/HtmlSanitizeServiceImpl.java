package com.liuhecai.service.impl;

import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.service.HtmlSanitizeService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class HtmlSanitizeServiceImpl implements HtmlSanitizeService {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .preserveRelativeLinks(true)
            .addTags("h1", "h2", "h3", "span", "s", "u")
            .addAttributes("span", "style")
            .addAttributes("p", "style")
            .addAttributes("div", "style")
            .addAttributes("h1", "style")
            .addAttributes("h2", "style")
            .addAttributes("h3", "style")
            .addAttributes("li", "style")
            .addAttributes(":all", "class")
            .addProtocols("a", "href", "http", "https", "#")
            .addProtocols("img", "src", "http", "https")
            .removeAttributes("img", "onerror", "onload");

    @Override
    public String sanitize(String html) {
        if (!StringUtils.hasText(html)) {
            return html;
        }
        // 纯文本无标签：包一层避免被 strip；Jsoup.clean 对纯文本会原样转义实体
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(html, "", SAFELIST, settings);
        cleaned = stripDangerousStyles(cleaned);
        cleaned = filterImgSrc(cleaned);
        cleaned = filterAnchorHref(cleaned);
        return cleaned;
    }

    @Override
    public boolean isSafeResourceUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return true;
        }
        String u = url.trim();
        String lower = u.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            return false;
        }
        if (u.startsWith("/")) {
            // 禁止协议相对 //evil.com
            return !u.startsWith("//");
        }
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    @Override
    public String requireSafeResourceUrl(String url, String fieldLabel) {
        if (!StringUtils.hasText(url)) {
            return url == null ? null : url.trim();
        }
        String u = url.trim();
        if (!isSafeResourceUrl(u)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldLabel + " URL 不合法");
        }
        if (u.toLowerCase(Locale.ROOT).endsWith(".svg")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldLabel + " 不支持 SVG");
        }
        return u;
    }

    private String stripDangerousStyles(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        for (Element el : doc.body().select("[style]")) {
            String style = el.attr("style");
            if (!StringUtils.hasText(style)) {
                continue;
            }
            String lower = style.toLowerCase(Locale.ROOT);
            if (lower.contains("expression") || lower.contains("javascript") || lower.contains("url(")
                    || lower.contains("behavior") || lower.contains("@import")) {
                el.removeAttr("style");
                continue;
            }
            // 仅保留字号/颜色/对齐
            StringBuilder keep = new StringBuilder();
            for (String part : style.split(";")) {
                String p = part.trim();
                if (p.isEmpty()) continue;
                String pl = p.toLowerCase(Locale.ROOT);
                if (pl.startsWith("font-size:") || pl.startsWith("color:")
                        || pl.startsWith("text-align:") || pl.startsWith("font-weight:")
                        || pl.startsWith("background-color:")) {
                    if (keep.length() > 0) keep.append(';');
                    keep.append(p);
                }
            }
            if (keep.length() == 0) {
                el.removeAttr("style");
            } else {
                el.attr("style", keep.toString());
            }
        }
        return doc.body().html();
    }

    private String filterImgSrc(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        for (Element img : doc.body().select("img[src]")) {
            String src = img.attr("src").trim();
            if (!isSafeResourceUrl(src) || src.toLowerCase(Locale.ROOT).endsWith(".svg")) {
                img.remove();
            }
        }
        return doc.body().html();
    }

    private String filterAnchorHref(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        for (Element a : doc.body().select("a[href]")) {
            String href = a.attr("href").trim();
            String lower = href.toLowerCase(Locale.ROOT);
            if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
                a.removeAttr("href");
            } else if (href.startsWith("//")) {
                a.removeAttr("href");
            }
        }
        return doc.body().html();
    }
}
