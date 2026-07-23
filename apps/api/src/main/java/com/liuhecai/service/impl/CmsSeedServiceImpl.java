package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.entity.SiteMenu;
import com.liuhecai.entity.SitePage;
import com.liuhecai.mapper.SiteMenuMapper;
import com.liuhecai.mapper.SitePageMapper;
import com.liuhecai.service.CmsSeedService;
import com.liuhecai.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CmsSeedServiceImpl implements CmsSeedService {

    private final SiteMenuMapper siteMenuMapper;
    private final SitePageMapper sitePageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void seedDefaultsIfEmpty(Long tenantId) {
        Long prev = TenantContext.get();
        try {
            TenantContext.set(tenantId);
            Long count = siteMenuMapper.selectCount(new LambdaQueryWrapper<SiteMenu>()
                    .eq(SiteMenu::getTenantId, tenantId));
            if (count != null && count > 0) {
                return;
            }
            seedDefaultsInternal(tenantId);
        } finally {
            if (prev == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(prev);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void seedDefaults(Long tenantId) {
        Long prev = TenantContext.get();
        try {
            TenantContext.set(tenantId);
            seedDefaultsInternal(tenantId);
        } finally {
            if (prev == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(prev);
            }
        }
    }

    private void seedDefaultsInternal(Long tenantId) {
        insertMenu(tenantId, "home", "首页", "/", 10, 1);
        insertMenu(tenantId, "rules", "规则", "/rules", 20, 1);
        insertMenu(tenantId, "recharge", "充值", "/recharge", 30, 1);
        insertMenu(tenantId, "kefu", "客服", "/kefu", 40, 1);
        insertMenu(tenantId, "login", "登录", "/login", 90, 0);
        insertMenu(tenantId, "register", "注册", "/register", 91, 0);

        insertPage(tenantId, "home", "首页", HOME_JSON);
        insertPage(tenantId, "rules", "充值与购买规则", RULES_JSON);
        insertPage(tenantId, "recharge", "金币充值", RECHARGE_JSON);
        insertPage(tenantId, "kefu", "客服联系", KEFU_JSON);
    }

    private void insertMenu(Long tenantId, String code, String title, String path, int sort, int visible) {
        SiteMenu m = new SiteMenu();
        m.setTenantId(tenantId);
        m.setCode(code);
        m.setTitle(title);
        m.setPath(path);
        m.setSortNo(sort);
        m.setVisible(visible);
        siteMenuMapper.insert(m);
    }

    private void insertPage(Long tenantId, String key, String title, String json) {
        SitePage p = new SitePage();
        p.setTenantId(tenantId);
        p.setPageKey(key);
        p.setTitle(title);
        p.setContentJson(json);
        sitePageMapper.insert(p);
    }

    private static final String HOME_JSON = """
            {
              "bannerUrl": "/bbs/images/311992.gif",
              "drawIframeUrl": "",
              "liveIframeUrl": "https://1e.36351c.com:8443/zb/kjzb.html",
              "domainBadge": "311992.com",
              "showLocalDrawPanel": true,
              "sisterSites": [
                {"name":"至尊无上论坛","domain":"305551.com","href":"https://305551.com","cta":"查看至尊无上","color":"#FF0000"},
                {"name":"神算子论坛","domain":"933858.com","href":"https://933858.com","cta":"查看神算子","color":"#22ac38"}
              ],
              "bottomImages": [
                {"src":"/bbs/promo/z129.png","alt":"广告条"},
                {"src":"/bbs/promo/sgxs.jpg","alt":"生肖攻略"},
                {"src":"/bbs/promo/vb4.jpg","alt":"资料图"},
                {"src":"/bbs/promo/ampgt.jpg","alt":"平特图"},
                {"src":"/bbs/promo/amttjs.jpg","alt":"特码图"},
                {"src":"/bbs/promo/alalx18m.jpg","alt":"连肖图"},
                {"src":"/bbs/promo/ammh.jpg","alt":"美化图"},
                {"src":"/bbs/promo/2025sxt.jpg","alt":"生肖图"},
                {"src":"/bbs/promo/di2.png","alt":"底图"}
              ],
              "qrWechatUrl": "/bbs/_root/weix.jpg",
              "qrQqUrl": "/bbs/_root/qq.jpg"
            }
            """;

    private static final String RULES_JSON = """
            {
              "heading": "充值与购买规则",
              "intro": "承诺与保障",
              "guarantees": [
                {"title":"保障一：","body":"本站无任何隐藏收费，购买资料按帖标价一次付清即可查看，杜绝二次收费。"},
                {"title":"保障二：","body":"出售资料均为开奖前发表，开奖后绝不更改对错结果，保证真实验证。"},
                {"title":"保障三：","body":"金币到账后即可自由选购，即时解锁，无需等待人工开贴。"},
                {"title":"保障四：","body":"站长只负责充值与监督，不售码、不荐码；资料由审核通过的高手发表。"},
                {"title":"保障五：","body":"购料纯属自愿，资料无100%中奖保证，请理性消费，量力而行。"}
              ]
            }
            """;

    private static final String RECHARGE_JSON = """
            {
              "heading": "【金币充值流程及注册说明】",
              "tiers": [
                "【充 500 元  送188  金币】",
                "【充1000元  送388  金币】",
                "【充2000元  送888  金币】",
                "【充5000元  送2888金币】",
                "【充10000元送5888金币】",
                "【充20000元送13888金币】"
              ],
              "exchangeRate": "（货币汇率：1人民币=1金币）",
              "declareText": "上方联系方式为论坛管理员。管理员不发表资料也不提供任何资料，论坛所有资料都是高手发表与版主无关。问码的请不要加！版主只提供充值服务，和监督高手!",
              "notes": [
                "（注意1：所有高手出售的资料都是单期购买才能查看）",
                "（注意2：金币充值后不允许退换,请谨慎考虑后再充值）"
              ],
              "qrWechatUrl": "/bbs/_root/weix.jpg",
              "qrQqUrl": "/bbs/_root/qq.jpg"
            }
            """;

    private static final String KEFU_JSON = """
            {
              "heading": "客服联系",
              "intro": "如需充值、查账或资料问题，请通过微信/QQ联系本站客服。版主只提供充值服务，不荐码。",
              "qrWechatUrl": "/bbs/_root/weix.jpg",
              "qrQqUrl": "/bbs/_root/qq.jpg"
            }
            """;
}
