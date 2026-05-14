# Novel Site Scrapers

## Criteria for New Sites

1. Not protected by Cloudflare (no CF challenge pages)
2. Supports search functionality
3. No rate limiting on search, or minimal interval that doesn't affect scraping
4. Content is accessible without login/authentication
5. Full chapter list is accessible without login

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
| 笔趣阁s | `Sbiquge` | www.sbiquge.com | GET `/search.php?q=` | Same template as Fsshu, URL pattern `/{cat}_{id}/` |
| 笔趣书 | `Bpshu` | www.bpshu.cc | GET `/search.php?q=` | Same template as Fsshu, URL pattern `/{id}/` |
| 笔趣奇 | `Biquqi` | www.biquqi.com | GET `/search.php?q=` | Same template as Fsshu, URL pattern `/book/{cat}/{id}/` |
| 全本小说网 | `QuanbenS` | www.quanben.io | GET `/index.php?c=book&a=search&keywords=` | Simplified Chinese variant of Quanben, pinyin slug URLs |
| 顶点小说网 | `Dingdian` | www.dingdian-xiaoshuo.com | GET `/?c=book&a=search.json2` | JSONP API with custom base64 encoding, pinyin slug URLs |

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
| 23求书 | www.23qb.net | 403 Forbidden |
| biquge.tw | www.biquge.tw | 403 Forbidden |
| 无法小说 | www.wfxs.tw | 403 Forbidden |

### Dead / Expired

| Site | URL | Status |
|------|-----|--------|
| 69书 | www.69shu.pro | Expired domain |
| biquge.lol | www.biquge.lol | ECONNREFUSED |
| xbiquge.la | www.xbiquge.la | ECONNREFUSED |
| shubaow | www.shubaow.net | ECONNREFUSED |
| ybdu | www.ybdu.com | ECONNREFUSED |
| 81book | www.81book.com | Redirects to 403 |
| txt99 | www.txt99.cc | Redirects to unrelated site |
| 23us | www.23us.cc | Now a movie site |
| biquge.info | www.biquge.info | ECONNREFUSED |
| bqzhh | www.bqzhh.com | Socket closed |
| bbiquge | www.bbiquge.net | ECONNREFUSED |
| xs74 | www.xs74.com | ECONNREFUSED |
| paoshu8 | www.paoshu8.com | ECONNREFUSED |
| xqishuta | www.xqishuta.com | ECONNREFUSED |
| bqg5200 | www.bqg5200.com | ECONNREFUSED |

### Accessible But No Search

| Site | URL | Encoding | Notes |
|------|-----|----------|-------|
| 顶点小说 | www.diandingnnn.cc | GB2312 | No search endpoint found, clean single-page chapters |
| 乐文小说 | www.lewenn.com | GB2312 | No search endpoint found, clean content |
| 笔趣阁789 | www.bqg789.net | UTF-8 | No search found (all patterns 404'd) |
| 爱看中文 | www.i25zw.com | UTF-8 | No search found |
| 笔趣阁u | www.biqvgeu.cc | GB2312 | No search found |
| 笔趣阁luge | www.biqluge.cc | GB2312 | search.php returns 404 |
| biquge321 | www.biquge321.com | UTF-8 | No search found, content works |

### Accessible But Content Issues

| Site | URL | Issue |
|------|-----|-------|
| 八推书屋 | www.8tsw.com | Chapter content has obfuscated/garbled Unicode characters |
| bigee | www.bigee.cc | Chapter pages use JS anti-bot challenge (cookie+redirect), search API non-functional |
| 5200小说 | www.5200xiaoshuo.com | Cloudflare 523 on search/detail pages, chapter content inconsistently accessible |
| biquges.cc | www.biquges.cc | Content garbled/wrong encoding |
