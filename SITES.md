# Novel Site Scrapers

## Implemented

| Site | Class | URL | Search | Notes |
|------|-------|-----|--------|-------|
| 飞速中文 | `Fsshu` | www.fsshu.com | GET `/search.php?q=` | UTF-8, chapter list paginated via `index_N.html`, content paginated via `_N.html` suffix |
| 笔趣集 | `Biquji` | www.biquji.com | GET `/search.php?q=` | Same template as Fsshu, URL pattern `/{cat}/{id}/` |
| 笔趣阁730 | `Bqg730` | www.bqg626.xyz | GET `/api/search?q=` | JSON API, mirror of www.bq730.cc, plain text content |
| 顶点笔趣阁 | `Ddbiquge` | www.ddbiquge.co | POST `/search.html` field `s` | Content is Base64-encoded in script tags, chapter list paginated via select dropdown |
| 得奇小说 | `Deqixs` | www.deqixs.co | GET `/modules/article/search.php?searchkey=` | Token-based AJAX content loading via chapter.js.php |
| 笔趣阁 | `Bqquge` | www.bqquge.com | GET `/so/{keyword}` | Content paginated via "下一页" link |
| 速读谷 | `Sudugu` | www.sudugu.org | GET `/i/sor.aspx?key=` | Chapter list paginated via `p-N.html`, content paginated via "下一页" link |
| 宏声小说 | `Hongsheng` | 8.140.232.133 | GET `/wap/?pbcode/so/{keyword}/` | Chapter list paginated via next link |
| 夏雨书屋 | `Xiayu` | www.xiayushuwu.com | GET `/search?keyword=` | Chapter list paginated via page-range links |
| 零点看书 | `Lingdian` | 23.225.121.243 | GET `/ar.php` | IP-based host, custom search endpoint |
| 繁體小說網 | `Xiaoshuo` | www.xiaoshuo.com.tw | GET `/modules/article/search.php?searchkey=` | GBK encoding, 403+cookie retry on first search request |
| 笔趣阁snapd | `Snapd` | www.snapd.net | GET `/api/search?q=` (via www.bqg474.cc) | JSON API on separate API host |
| 愛下電子書 | `Ixdzs` | ixdzs.tw | GET `/bsearch?q=` | Traditional Chinese ebook site |
| 全本小說網 | `Quanben` | big5.quanben.io | GET `/index.php?keyword=` | Traditional Chinese, Big5 variant |
| 笔趣阁365 | `Biquge365` | www.biquge365.net | POST `/s.php` field `keyword` | Standard biquge template |
| 错层小说 | `Cuoceng` | m.cuoceng.com | GET `/book/so/{keyword}.html` | Mobile-oriented site |

## Explored — Cannot Scrape

### Cloudflare / 403 Forbidden

| Site | URL | Status |
|------|-----|--------|
| xbiquge | www.xbiquge.so | 403 Forbidden |
| 书趣阁 | www.shuquge.com | ECONNREFUSED |
| 趣啦 | www.qu-la.com | 403 Forbidden |
| ibiquge | www.ibiquge.info | ECONNREFUSED |
| 和图书 | www.hetushu.com | 403 Forbidden |
| UU看书 | www.uukanshu.cc | 403 Forbidden |
| 226看书 | www.226ks.com | ECONNREFUSED |
| 飘天文学 | www.ptwxz.com | 403 Forbidden |
| 妙笔阁 | www.imiaobige.com | ECONNREFUSED |
| 52笔趣阁 | www.52bqg.org | Socket closed |
| 搜小说 | www.soxs.cc | ECONNREFUSED |
| biqu-ge | www.biqu-ge.org | ECONNREFUSED |
| 小说旺 | www.xswang.net | 403 Forbidden |
| readwn | www.readwn.org | 403 Forbidden |
| 笔趣阁5200 | www.biquge5200.com | ECONNREFUSED |
| 69书吧 | www.69shuba.com | 403 Forbidden |
| 笔趣阁s | www.biquges.net | 403 Forbidden |
| xbiquwk | www.xbiquwk.com | ECONNREFUSED |
| xbiquge.cc | www.xbiquge.cc | ECONNREFUSED |
| 笔趣看 | www.biqukan.com | 403 Forbidden |
| bqkan8 | www.bqkan8.com | 403 Forbidden |
| 文库8 | www.wenku8.net | 403 Forbidden |
| 顶点小说 | www.ddxs.com | 403 Forbidden |
| ttshu | www.ttshu.cc | 403 Forbidden |
| kunnu | www.kunnu.com | 403 Forbidden |

### Dead / Expired

| Site | URL | Status |
|------|-----|--------|
| 69书 | www.69shu.pro | Expired domain |

### Accessible But No Search

| Site | URL | Encoding | Notes |
|------|-----|----------|-------|
| 顶点小说 | www.diandingnnn.cc | GB2312 | No search endpoint found, clean single-page chapters |
| 乐文小说 | www.lewenn.com | GB2312 | No search endpoint found, clean content |
| 笔趣阁789 | www.bqg789.net | UTF-8 | No search found (all patterns 404'd) |
| 爱看中文 | www.i25zw.com | UTF-8 | No search found |
| 笔趣阁u | www.biqvgeu.cc | GB2312 | No search found |
| 笔趣阁luge | www.biqluge.cc | GB2312 | search.php returns 404 |

### Accessible But Content Issues

| Site | URL | Issue |
|------|-----|-------|
| 八推书屋 | www.8tsw.com | Chapter content has obfuscated/garbled Unicode characters |
