# 鐏电妧Hook

> 鍩轰簬 libxposed 102 鐜颁唬 API 鐨?vivo / iQOO 绯荤粺琛ヤ竵锛岃窇鍦?OriginOS 涓婏紝鎷︽埅绯荤粺鎸夋満鍨嬮槈鍓茬殑鍒ゅ畾锛屾妸琚殣钘忕殑鍏ュ彛涓庤兘鍔涙斁寮€銆傚綋鍓嶅凡瑕嗙洊 **鐪佺數绠＄悊銆佺浉鏈猴紙ZEISS 姘村嵃/鍥炬爣/鏍″洯/楂樺儚绱狅級銆佺湡瀹炵數閲忋€佹櫤鎱у紩鎿?ABE 鑷姩閲嶅惎灞忚斀銆佺數婧愪俊鎭?FuelSummary 鍏呯數/瀹归噺/寰幆銆佹父鎴忛瓟鐩掞紙Q 鑺墖涓撳尯/鍏夎拷/TAA锛夈€佽繙绋嬫洿鏂?*锛屽悗缁寜闇€鎵╁睍鑷虫洿澶氱郴缁熸ā鍧椼€?

| 浜嬮」 | 璇存槑 |
|---|---|
| 鍖呭悕 | `github.boxiaolanya2008.lingxihook` |
| 鏈€浣?SDK | 33锛坢iuix-blur 寮哄埗锛夛紝鐩爣 37锛孉GP 9.3.1锛孠otlin 2.4.10 |
| 妗嗘灦 | LSPosed / LSPatch 绛夋敮鎸?libxposed 101+ 鐨勫疄鐜?|
| UI | Compose BOM 2026.02.01 + Material 3 1.5 Expressive + materialkolor 4.1.1 + miuix-blur 0.9.3锛堟恫鎬佺幓鐠冨鑸級 |
| 鐗堟湰 | `3` `1.3.0`锛坄update.json` 杩滅▼鍙帶锛宍force_update` 鍖哄垎寮哄埗/鍙◢鍚庯級 |

## 瀹炴満婕旂ず

> V2520A OriginOS 鐪熸満锛孡SPosed 宸插嬀閫夊搴斾綔鐢ㄥ煙

| 涓婚〉路宸查€傞厤 | 璇︽儏椤德锋按鍗?鍏呯數 |
|---|---|
| <img src="image/image1.jpg" width="360" /> | <img src="image/image2.jpg" width="360" /> |

## 宸查€傞厤

- **鐪佺數绠＄悊锛坴ivo / iQOO锛塦com.iqoo.powersaving`**
  - **鏃犵嚎鍏呯數閫傞厤**锛坄HookFeature` `lingxi_hook_wireless`锛岄粯璁ゅ紑锛夛細Hook `com.iqoo.powersaving.utils.g#E` / `#F(Context)` 寮哄埗 `true`锛屾斁琛?`persist.vivo.wireless_charge_support` 涓?`wireless_position_support` 鍒ゆ柇锛屾仮澶嶈缃〉鈥滃弽鍚戞棤绾垮厖鐢碘€濃€滄棤绾垮厖鐢垫憜鏀句綅缃€濅袱椤广€?
  - **鍏抽棴搴旂敤娣卞害浼樺寲**锛坄lingxi_hook_deepopt`锛岄粯璁ゅ紑锛夛細Hook `appoptimize.b/d#startDexoptJob` 杩斿洖 `false`銆乣getDexoptPackages` 绌哄垪琛ㄣ€乣getPredictDexoptTime` 0銆乣getRunningStatus` `OPTIMIZED_MANUAL`锛岄樆鏂悗鍙?dexopt 璋冨害涓庘€滀竴閿紭鍖栤€濇寜閽€?
  - **鐢垫睜璇︽儏鍗?*锛坄HookFeature` `lingxi_hook_power_battery_card`锛岄粯璁ゅ紑锛夛細Hook `PowerRankActivity#H0`/`I(*Toolbar)`/`Activity.onResume`锛屽湪鑰楃數鎺掕椤碘€滃綋鍓嶇數閲忊€濅箣涓婃彃鍏ョ數姹犺鎯呭崱锛堝ぇ鐢甸噺 + 鍏呮斁鐢电姸鎬?+ 棰勪及鍙敤鏃堕暱 + 娓╁害/鐢靛帇/鐢垫祦/鍋ュ悍搴?瀹归噺/寰幆锛? 绉掑埛鏂帮級锛涙壘涓嶅埌娉ㄥ叆鐐归潤榛樿烦杩囥€?
- **鐩告満锛坴ivo / iQOO锛塦com.android.camera`**
  - **ZEISS 姘村嵃瑙ｉ攣**锛坄HookFeature` `lingxi_hook_camera_zeiss`锛岄粯璁ゅ紑锛夛細Hook `featureconfig.FeatureConfig_common#isCameraSignedByZeiss/isSupportWatermarkZEISS/isSupportWatermarkBorder/isSupportZeissColor` 寮哄埗 `true`锛宍supportWatermarkTmpl` 鍙嶅皠鍙?`WMTmplID` 鍏ㄩ噺锛堝惈 `BORDER_PHOTO_AURALIGHT/BORDER_PHOTO_AURALIGHT_V/MASTER_PHOTO_AURALIGHT_V` 绛?AURA LIGHT 鍙樹綋锛夊苟鎶?`getWatermarkVersion` 鎶嚦 `V4`锛宍StandardSizeConfig#isIqooLogoName -> false` 涓?`DeviceUtil#isIQOO`锛堜粎姘村嵃鏍堬級`-> false`锛涘悓鏃朵吉瑁呮満鍨嬩负 `vivo X500 BETA`锛坄Build.MODEL/PRODUCT/DEVICE`銆乣SystemProperties ro.product.model.bbk/ro.vivo.market.name/ro.vivo.product.series=X/platform=MT6991`銆乣FeatureConfig#getMarketName/getVivoLogoName/productSeries`銆乣WatermarkUtils#generateLogoText` 鍏ㄩ噺鎷︽埅锛宍productBatchTime=20250930`锛夛紝鎽勫儚鍙傛暟瀵归綈 `vivo X300 Pro`锛堝ぉ鐜?9400 MT6991 鏃楄埌锛夛紝浣?iQOO 鎴愮墖 EXIF 涓庤竟妗嗘按鍗板潎钀界洏 `ZEISS` 鑱斿悕鍥炬爣涓?`vivo X500 BETA | ZEISS` 钀芥銆?
  - **姘村嵃鍥炬爣鍏ㄦ樉**锛坄HookFeature` `lingxi_hook_camera_icons`锛岄粯璁ゅ紑锛夛細Hook `FeatureConfig_common#isSupportShowWatermarkIcon -> true` 鏀捐鍥炬爣鏍忥紝鎷︽埅鎵€鏈?`WMTemplate.WMItem/RelatedWMItem` 鏋勯€犲櫒鎶?`unShowList` 娓呯┖涓?`HashSet(0)` 骞?`show=true`锛宍getIQOOBorderWatermarkImageVersion->2` 涓?`getIQOOBorderWatermarkImageOrder->[threecolor_logo/iqoo_logo/kpl_logo]` 琛ュ叏 IQOO 涓夊浘鏍囷紝骞跺弽灏勮ˉ涓佸凡寤哄ソ鐨?`gk/j` 闈欐€佹睜 `Map<String,WMTemplate>` 涓叏閮?`unShowList`锛涘鍘熺己 `LOGO_PIC` 鐨?`BORDER_PHOTO / BORDER_PHOTO_AURALIGHT` 鍔ㄦ€佹敞鍏?`R.array.pref_camera_watermark_boder_logo_pics` 鐨?`WMItem(LOGO_PIC, pref_camera_water_mark_logo_pic)`锛屼娇**杈规姘村嵃**椤碉紙濡傚浘鍦堥€夌殑 `AURA LIGHT` 鍦嗗舰寰芥爣锛夋柊澧炲彲妯粦鐨?*姘村嵃鍥炬爣**閫夋嫨鍣紝鏀寔鍦?`ZEISS / vivo / AURA LIGHT / KPL` 绛夊叏閮ㄥ畼鏂瑰浘鏍囬棿鍒囨崲锛屼笉鍐嶆寜鏈哄瀷闃夊壊銆?
  - **鏍″洯姘村嵃淇**锛坄HookFeature` `lingxi_hook_camera_campus`锛岄粯璁ゅ紑锛夛細Hook `ISettingManager#getSettingValueFromKey(pref_camera_watermark_graduate_school)` 绌?`normal`/`7` 鍥為€€涓?`娴欐睙澶у`锛屽苟鎷︽埅 `oi/f#beforeOnItemClick` 瀵?`GRADUATE_SCHOOL` 妯℃澘鑷姩鍐欏叆榛樿瀛︽牎缁曡繃绌哄鏍″脊绐楋紝浣挎牎鍥按鍗帮紙鍗庝腑绉戝ぇ/娴欏ぇ锛夐€夋嫨鍚庡彲鐩存帴鍑虹墖骞舵甯歌惤鐩樿竟妗嗐€佹牎寰戒笌鍙ｅ彿銆?
  - **楂樺儚绱犺В閿?50M鈫?00M**锛坄HookFeature` `lingxi_hook_camera_highpixel`锛岄粯璁ゅ紑锛夛細Hook `FeatureConfig#getSupportRemosaicValue(Master 32鈫?00/Wide 32鈫?0/Tele 32鈫?00)` 涓?`isSupport200MP/isSupportPhotoHighResolution` 鍏ㄩ噺 `true`锛屽苟鎷︽埅 `CameraCharacteristics#get(SENSOR_INFO_PIXEL_ARRAY_SIZE)` 涓?`ISettingManager` 鎸佷箙鍖栭敭 `remosaic/high/pixel`锛屼娇 V2520A 鍘?50M 涓绘憚鍦ㄢ€滈珮鍍忕礌鈥濅腑鍑虹幇 100M/200M 妗ｄ綅涓旈噸鍚悗浠嶄负 200M锛屽彇鏅櫒鍒囧埌 200M 鍚庢寜 `SENSOR_PIXEL_MODE` 鎵撳寘钀界洏銆?
- **鐪熷疄鐢甸噺锛堢郴缁燂級`android`**锛坄lingxi_hook_real_battery`锛岄粯璁ゅ紑锛夛細Hook `system_server BatteryService` 娲惧彂 `level` 浣块鏍间笉鍐?30~60m 鍚庢毚璺岋紝`UI=FG raw_soc` 鍧囧寑 10m/鏍硷紙鍒疯棰戯級/5m锛圡OBA锛夛紝宸插湪 `SystemHook.kt + BatteryRealHook.kt` 瀹炵幇銆?
- **绯荤粺鏇存柊灞忚斀锛堟墜鍔級`android`**
  - **灞忚斀绯荤粺鏇存柊**锛坄HookFeature` `lingxi_hook_block_update`锛岄粯璁ゅ叧锛夛細涓嶈嚜鍔?Hook锛岄渶 ROOT 鎵嬪姩鎵ц鏇存柊椤垫彁绀虹殑 `setprop` 鍛戒护锛屽紑鍏充粎浣滃叆鍙ｄ笌 Root 妫€娴嬫彁绀恒€?
  - 鍘熸満鍨嬩吉瑁呭姛鑳斤紙PD2520鈫扨D2502 / V2520A鈫扸2502A锛?*宸茬Щ闄?*锛歴ystem_server 鍏ㄥ眬浼浼氬鑷翠簯閰嶇疆鎸夐敊璇満鍨嬩笅鍙戙€乨aemon 鍚嶅崟閲嶇亴閿欎贡銆?
- **鏅烘収寮曟搸锛坴ivo ABE / Smart Engine锛塦com.vivo.abe`**
  - **灞忚斀鑷姩閲嶅惎**锛坄HookFeature` `lingxi_hook_abe_silent_reboot`锛岄粯璁ゅ紑锛夛細Hook `com.vivo.silentreboot.SilentRebootService#p0/o0/g0/v0/n0/Z` 闃绘柇 02:00-04:00 澶滈棿闈欓粯閲嶅惎璋冨害涓?`AlarmManager.setExactAndAllowWhileIdle`锛屽苟鎷︽埅 `android.os.PowerManager#reboot(silent/reboot)` 涓?`e4.a#i() sysrb` 绛栫暐閲嶅惎锛宍com.vivo.abe` 杩涚▼鍐呯洿鎺?`PROTECTIVE` 鎷︽埅涓嶆墽琛岋紝闇€ LSPosed 鍕鹃€?`com.vivo.abe`銆傜郴缁熷簲鐢ㄥ悕绉版樉绀轰负鈥滄櫤鎱у紩鎿庘€濄€?
- **鐢垫簮淇℃伅锛坴ivo FuelSummary锛塦com.vivo.fuelsummary`**锛坄lingxi_hook_fuel_*`锛岄粯璁ゅ紑锛?
- **vivo 绀惧尯锛坈om.vivo.space锛塦com.vivo.space`**
  - **娑叉€佺幓鐠冨鑸爮锛堟浛鎹級**锛坄HookFeature` `lingxi_hook_space_glass_nav`锛岄粯璁ゅ紑锛夛細Hook `VivoSpaceTabActivity#onPostCreate`锛屾妸鏈ā鍧?ComposeView锛堝畬鏁存恫鎬佺幓鐠冨鑸細娴佸厜/绮掑瓙/鎷栧姩鍥炲脊锛夋寕鍒扮獥鍙ｅ簳閮紝鍘?`SpaceVBottomNavigationView` GONE 闅愯棌锛涜兌鍥婃崲椤电粡 `VMenuViewLayout` 瀛愰」 `performClick` 妗ユ帴鍘熼〉闈紱娉ㄥ叆澶辫触鑷姩鍏滃簳鍘熺敓鐜荤拑鍖栬兌鍥婃牱寮忥紙鍗婇€忔槑鍦嗚搴?+ 鎮诞 + 鎶曞奖锛夈€傞檺鍒讹細Compose 鎶樺皠灞傞噰鏍蜂笉鍒板涓诲師鐢?View 鐢婚潰锛岀幓鐠冭儗鏅负绾壊鍏滃簳銆?
  - **骞垮憡灞忚斀**锛坄HookFeature` `lingxi_hook_space_adblock`锛岄粯璁ゅ紑锛夛細灞忚斀寮€灞?鍏ㄥ睆娴眰骞垮憡锛坄logo_adv_layout` / `dialog_pag_view` / `popup_container`锛変笌棣栭〉鎺ㄨ崘娴佸箍鍛婃í鏉★紙`RecommendPageRecyclerAdapter` viewType `0/23/28/33/34`锛宩adx 鍙嶇紪璇戠‘璁わ級锛涗綆棰戣疆璇㈤槻骞垮憡鍙嶅脊銆?
  - **绉婚櫎鍏呯數闄愭祦**锛坄lingxi_hook_fuel_charging_unlimit`锛岄粯璁ゅ紑锛夛細Hook `r0.f#K/M/H + h()/f0/F + y/z + 鐢垫祦娓╂帶` 寮哄埗瓒呭揩鍏呮敮鎸?`true`銆佹櫤鑳介檺娴?`false`銆佹俯鎺ч槇鍊?`42鈫?0鈩僠 骞惰繃婊?`h#r/L` 鐨?`fex_* / fix_temp` 闄愭祦鍐欏叆锛屽叏绋嬩笉闄嶉€燂紙椋庨櫓鑷媴锛夈€?
  - **鐢垫睜瀹归噺閿佹渶澶?*锛坄lingxi_hook_fuel_capacity_max`锛岄粯璁ゅ紑锛夛細Hook `battery.health.a#b/c + g#f/d + h#u/m/C` 瀵?`capacity_mah/soh` 鑺傜偣寮哄埗 `100`锛屽仴搴峰害涓庡閲忓缁堟樉绀烘弧琛€銆?
  - **寰幆娆℃暟閿?5 娆?*锛坄lingxi_hook_fuel_cycle_5`锛岄粯璁ゅ紑锛夛細Hook `g#b + h#z/C/u/m` 瀵?`/sys/class/fuelsummary/cycle` 寮哄埗 `5`锛屽仴搴锋洸绾挎寜 5 娆¤绠椼€?
- **娓告垙榄旂洅锛坴ivo gamecube锛塦com.vivo.gamecube`**
  - **閰嶇疆璺緞閲嶅畾鍚?*锛坄HookFeature` `lingxi_hook_gamecube_config`锛岄粯璁ゅ紑锛夛細Hook `FileInputStream/FileReader` 鏋勯€犲櫒锛屾寜鏂囦欢鍚嶆嫤鎴?`FrameInterConfigList.xml / SuperResolutionConfigList.xml`锛屾父鎴忚嚜瀹?`Android/data/com.vivo.gamecube/files/gamecube/` 涓嬫湁鍚屽悕鍙鏂囦欢鍗虫崲璺紙MT 绠＄悊鍣ㄥ彲鍐欏叆锛岃嚜瀹剁洰褰曞厤鏉冮檺锛夛紝娌℃湁鍒欑収鏃ц `/system/etc/gamecube/`锛屽紑鍏?5 绉?TTL 缂撳瓨涓嶆嫋鐑矾寰勩€?
  - **Q鑺墖涓撳尯鍏ュ彛寮哄埗寮€鍚?*锛坄HookFeature` `lingxi_hook_gamecube_qzone`锛岄粯璁ゅ紑锛夛細涓冨鎷︽埅琛ラ綈闈炴棗鑸扮己鍙ｂ€斺€斿姛鑳界櫧鍚嶅崟鎬婚椄 `e0#h0` 瀵?`boost_frame*/optimize_power*/super_resolution/game_super_hd_engine` 寮哄埗 `true`锛沗e0#p0` 鏀捐鎻掑抚鏉＄洰锛堣ˉ MEMC 纭欢鐗规€х己澶?`vivo.hardware.game.memc`锛夛紱`frameinterpolation.k#m` 涓庡疄闄呰矾寰?`hb.j#c` 鍙屽厹搴曪紙`f21156h` 鍒楄〃浠?MEMC 鏈哄姞杞姐€乂2520A 鎭掔┖锛宍QSuperFrameView` 鏋勯€犲己瑙ｅ紩鐢?null 浼氬穿 assistantui锛夛紝鏌ヤ笉鍒版父鎴忔椂杩斿洖榛樿 ga.b锛?0鈫?44/鏃燬DK锛夛紱`hb.j#f/g`锛堟瀬鑷村抚鐜?鐪佺數甯х巼鎸″彲瑙佹€э級寮哄埗 `true`锛沗com.vivo.common.a#z0()`锛坰upersr 鐗规€?nova锛変笌 `e0#n0`锛坅isr 鍒楄〃闈?nova 鏈烘亽绌猴級寮哄埗 `true` 鏀惧嚭鏋佽嚧鍒嗚鲸鐜囨尅锛沗l#l2`锛堢郴缁熷垎杈ㄧ巼妯″紡妫€娴嬶紝涓庢父鎴忓唴娓叉煋绮惧害鏃犲叧锛夊己鍒?`true` 鍘绘帀鈥滆璋冮珮绯荤粺瓒呭垎绛夌骇鈥濇彁绀猴紝浠绘剰娓告垙瓒呭垎/鎻掑抚鍏ㄩ儴鎸′綅涓嶅啀鎸夋満鍨嬮槈鍓层€?
  - **Q鑺墖涓撳尯鍏ュ彛寮哄埗寮€鍚?*锛坄HookFeature` `lingxi_hook_gamecube_qzone`锛岄粯璁ゅ紑锛夛細涓冨鎷︽埅琛ラ綈闈炴棗鑸扮己鍙ｂ€斺€斿姛鑳界櫧鍚嶅崟鎬婚椄 `e0#h0` 瀵?`boost_frame*/optimize_power*/super_resolution/game_super_hd_engine` 寮哄埗 `true`锛沗e0#p0` 鏀捐鎻掑抚鏉＄洰锛堣ˉ MEMC 纭欢鐗规€х己澶?`vivo.hardware.game.memc`锛夛紱`frameinterpolation.k#m` 涓庡疄闄呰矾寰?`hb.j#c` 鍙屽厹搴曪紙`f21156h` 鍒楄〃浠?MEMC 鏈哄姞杞姐€乂2520A 鎭掔┖锛宍QSuperFrameView` 鏋勯€犲己瑙ｅ紩鐢?null 浼氬穿 assistantui锛夛紝鏌ヤ笉鍒版父鎴忔椂杩斿洖榛樿 ga.b锛?0鈫?44/鏃燬DK锛夛紱`hb.j#f/g`锛堟瀬鑷村抚鐜?鐪佺數甯х巼鎸″彲瑙佹€э級寮哄埗 `true`锛沗com.vivo.common.a#z0()`锛坰upersr 鐗规€?nova锛変笌 `e0#n0`锛坅isr 鍒楄〃闈?nova 鏈烘亽绌猴級寮哄埗 `true` 鏀惧嚭鏋佽嚧鍒嗚鲸鐜囨尅锛沗l#l2`锛堢郴缁熷垎杈ㄧ巼妯″紡妫€娴嬶紝涓庢父鎴忓唴娓叉煋绮惧害鏃犲叧锛夊己鍒?`true` 鍘绘帀鈥滆璋冮珮绯荤粺瓒呭垎绛夌骇鈥濇彁绀猴紝浠绘剰娓告垙瓒呭垎/鎻掑抚鍏ㄩ儴鎸′綅涓嶅啀鎸夋満鍨嬮槈鍓层€?
  - **鏄剧ず璁剧疆寮哄埗寮€鍚?*锛坄HookFeature` `lingxi_hook_gamecube_display`锛岄粯璁ゅ紑锛夛細鍏ュ彛鎬诲紑鍏?`e0#T0()` 寮哄埗 `true`锛堝師鍒ゅ畾 `j0(DISPLAY_SETTINGS)` 璧拌澶囩骇鏈哄瀷鐧藉悕鍗曚粎鍒?PD2254锛? `e0#h0` 瀵?`display_settings` 鍏滃簳鏀捐锛屾壘鍥為瓟鐩掑唴娑堝け鐨勨€滄樉绀鸿缃€濄€?
  - **娓告垙鍏夎拷寮哄埗寮€鍚?*锛坄HookFeature` `lingxi_hook_gamecube_lighttrack`锛岄粯璁ゅ紑锛夛細鎷︽埅 `com.vivo.common.utils.m0#f`锛坄Settings.Global xpq_whitelist_apps` 鍏夎拷鐧藉悕鍗曞垽瀹氾級寮哄埗 `true`锛屼换鎰忔父鎴忔斁鍑?Q 鍖衡€滃厜褰辫拷韪€濇潯鐩紱搴曞眰 XPQ 鏈嶅姟鏁堟灉闇€纭欢鏀寔锛屼互瀹炴祴涓哄噯銆?
  - **TAA / 鎶楅敮榻垮己鍒跺紑鍚?*锛坄HookFeature` `lingxi_hook_gamecube_taa`锛岄粯璁ゅ紑锛夛細鎷︽埅 `e0#F0/t0/B0` 涓夊纭紪鐮佹父鎴忓悕鍗曪紙鍘熺/鏄熼搧/缁濆尯闆?楦ｆ疆锛変笌 q3.0 寮曟搸鐗堟湰鍒ゅ畾锛屽己鍒?`true` 浣夸换鎰忔父鎴忔斁鍑?TAA 涓庢姉閿娇閫夐」锛涜繛甯︽斁琛?`e0#l0` 娓叉煋绮惧害鏍￠獙锛堝師绁?3"寮€澶?鏄熼搧"4"妗ｄ綅锛岄潪鏃楄埌璇讳笉鍒板疄鏃跺€兼亽鎷︼級锛屽垏鎸′笉鍐嶅脊鈥滆灏嗘覆鏌撶簿搴﹁皟鑷抽珮鈥濄€?  - **Root 鑷巿鏉?*锛氭湰 App 鍚姩鏃惰嫢妫€娴嬪埌 Root锛岃嚜鍔?`appops` 鎷夸笅鏈寘 `WRITE_SETTINGS`锛堢渷鎺夋墜鍔ㄨ烦鎺堟潈椤碉級涓?gamecube `MANAGE_EXTERNAL_STORAGE`锛堜娇鍏惰兘鐩磋 sdcard 瑕嗙洊鐩綍锛夛紝缁撴灉鎵?`[rootauth]` 鏃ュ織锛涚紪杈戝櫒搴旂敤鍒楄〃闇€ `QUERY_ALL_PACKAGES`锛堝凡澹版槑锛夛紝鍚﹀垯 Android 11+ 鍙兘鐪嬪埌绯荤粺搴旂敤銆?
- **杩滅▼鏇存柊** `update.json`锛坄Gitee raw` 鏃犵紦瀛橈級
  - 5 鏍囪瘑 `versionCode / versionName / force_update / download_url / changelog`锛宍UpdateChecker.kt:19` 姣忔 `?t=` + `no-cache/no-store` 寮哄埗璧扮綉缁滐紝`force_update=true` 鏃跺脊绐楁棤鈥滀笅娆″啀璇粹€濅笖涓嶅彲鍙栨秷锛宍false` 鏃跺彲绋嶅悗銆?

寮€鍏冲啓鍏?`Settings.System` 闀滃儚锛宍HookConfig` 鍦ㄧ洰鏍囪繘绋嬪疄鏃惰鍙栵紝涓嶉渶閲嶅惎锛涙湭鎺堜簣鈥滀慨鏀圭郴缁熻缃€濇椂鎸夐粯璁ゅ€肩敓鏁堛€俙HookLogger` 缁熶竴鎵?`LingXiHook` 鍒?logcat锛岃嚜韬洿鍐?`filesDir/logs/lingxi.log`锛岀洰鏍囪繘绋嬩紭鍏堢洿鍐?`/data/local/tmp/lingxihook.log`锛坄world-writable` 缁曞箍鎾檺锛夊け璐ュ啀骞挎挱 `LogReceiver`锛宍LogRepo` 鍚堝苟鍙屾枃浠讹紝鏃ュ織椤?2s 鑷姩鍒锋柊鍙瓫 `INFO/WARN/ERROR`锛宍[abe]`/`[fuel]`/`[real]` 鏃犻渶 `adb logcat`銆傚簲鐢ㄥ惎鍔ㄦ椂 `MainActivity.kt:37` 閫氳繃 `su -c id` 妫€娴?Root锛屾湭 Root 寮?`缁х画浣跨敤/閫€鍑哄簲鐢╜ 璀﹀憡銆?

## 鐜

- Android Studio Ladybug 浠ヤ笂锛孞DK 17锛宍sdk.dir` 鎸囧悜 `D:\as-sdk`
- 鐪熸満宸插埛 LSPosed锛屼綔鐢ㄥ煙鍕鹃€?`com.iqoo.powersaving` / `com.android.camera` / `android` / `com.vivo.abe` / `com.vivo.fuelsummary` / `com.vivo.space` / `com.vivo.gamecube` 涓庢湰鍖呰嚜韬紝鏇存柊鐩告満/ABE/FuelSummary 鍚庨渶鍦?LSPosed 涓噸鏂板嬀閫夊搴斿寘鍚?

## 蹇€熷紑濮?

```bash
git clone <repo> && cd LXHook
./gradlew :app:assembleDebug
./gradlew :app:installDebug   # 宸茶繛 V2520A 鏃剁洿鎺ヨ鍒拌澶?
adb logcat -s LingXiHook       # 鐪?[powersaving][wireless][deepopt] / [camera][zeiss][icons][campus][highpixel][model][device][abe][fuel][update] 娉ㄥ叆鏃ュ織
# 鎵嬪姩灞忚斀鏇存柊锛堥渶 Root锛夛細adb shell su -c setprop persist.sys.u.debug true && su -c setprop persist.sys.u.server.addr http://127.0.0.1:9/  杩樺師锛歞ebug鈫抐alse/绌猴紝addr鈫掔┖
```

鏀?Hook 鍚庡彧鏀逛袱澶勶細`hook/{app}/XXXHook.kt` 瀹炵幇 `install`锛宍HookRegistry.kt:11` 鍔犱竴琛岋紝瀵瑰簲涓?Hook `VivoCameraHook/IqooPowerSavingHook/SystemHook/VivoAbeHook/VivoFuelSummaryHook/VivoGameCubeHook` 鐨?`features` 鍔犱竴椤癸紝棣栭〉鑷姩鍑虹幇寮€鍏炽€備緥锛歚hook/camera/HighPixelHook.kt` + `hook/device/SystemHook.kt` + `hook/abe/SilentRebootHook.kt` + `hook/fuelsummary/ChargingSpeedHook.kt` + `hook/gamecube/QZoneGateHook.kt` + `HookRegistry.kt:16` + `VivoCameraHook.kt:22 lingxi_hook_camera_highpixel / SystemHook.kt:19 lingxi_hook_real_battery / VivoAbeHook.kt:18 lingxi_hook_abe_silent_reboot / VivoFuelSummaryHook.kt:18 lingxi_hook_fuel_* / VivoGameCubeHook.kt:18 lingxi_hook_gamecube_config/_qzone`銆?

## 涓婚

`MainActivity` 鎸佹湁 `colorMode/keyColor/paletteStyle` 涓夌姸鎬侊紝`鐏电妧HookTheme` 鐢?`rememberLingXiColorScheme`锛坄keyColor==0` 鏃跺彇 Monet 涓昏壊鎸夋墍閫?`PaletteStyle` 閲嶇畻鑹叉澘锛? `G2Shapes(large=16.dp)` + `MotionScheme.expressive()`锛屽垏涓婚鏃?`ColorScheme.animateAsState()` 鐢?`spring` 鍏ㄩ噺娓愬彉銆?

## 鎻愪氦瑙勮寖

璇﹁鐙珛鏂囦欢 [`COMMIT_CONVENTION.md`](./COMMIT_CONVENTION.md)銆傚己鍒朵互 `lxhook: ` 鎴?`lxhook(scope): ` 寮€澶达紝渚嬪 `lxhook: 鏂板 deepopt 绌哄垪琛ㄦ嫤鎴猔銆乣lxhook(powersaving): 淇 palette 涓嶈窡闅廯锛屾鏂囧繀椤诲啓娓呮敼鍔ㄦ枃浠惰鍙蜂笌 `assembleDebug` + `V2520A installDebug` 楠岃瘉锛屽垎鏀悕 `feat/xxx-鏃ユ湡` 涓庨涓?commit 鏍囬涓€鑷达紝鍚堝苟鐢?`gh pr merge --squash`銆?

## 鍒嗘敮涓庡悎骞讹紙缁?AI 妯″瀷锛?

鏈珷鏄粰鍚庣画鎺ユ墜鐨?AI 鍐欑殑纭€ф搷浣滄墜鍐岋紝鎸夋璧板彲閬垮厤鎶?`master` 寮勮剰銆?

### 1. 鍒嗘敮鍛藉悕

- `feat/xxx` 鏂?Hook/鏂伴〉闈紝`fix/xxx` 淇锛宍chore/xxx` 鏋勫缓/渚濊禆锛宍ai/xxx` AI 鎵归噺鏀瑰姩銆傚悗闈㈣窡鏃ユ湡鎴栭渶姹?ID锛屼緥濡?`feat/deepopt-20260831`銆乣fix/palette-check-20260831`銆?
- 绂佹鐩存帴鍦?`master` 涓?`commit`銆?

### 2. 鍒涘缓鍒嗘敮

```bash
git fetch origin
git checkout master
git pull --ff-only origin master
git checkout -b feat/浣犵殑闇€姹?20260831
# 鏍稿 scope.list 涓?arrays.xml 鍙屼唤浣滅敤鍩熶竴鑷?
./gradlew :app:assembleDebug
```

### 3. 寮€鍙戣嚜妫€

- `utils.g#E/F` 鎴?`appoptimize.b#startDexoptJob` 杩欑被娣锋穯鏂规硶涓㈠け鏃跺彧鎵?`WARN`锛屼笉璁╃洰鏍囧簲鐢ㄥ穿婧冿紝淇濇寔 `PROTECTIVE`銆?
- 鏀?UI 鍗＄墖蹇呴』璧?`SegmentedColumn` + `SegmentedListItem`锛坄surfaceContainerHighest` 閰?`G2 16.dp`锛夛紝绛涢€変竴寰?`SingleChoiceSegmentedButtonRow` 甯?`Icon(active=selected)` 鐨?鉁撱€?
- `TonalCard` 浠呭吋瀹癸紝绂佹鏂颁唬鐮佸啀鐢ㄣ€?

### 4. 鎻愪氦

```bash
git status
git diff
git add app/src/main/java/github/boxiaolanya2008/lingxihook/hook/powersaving/DeepOptimizationHook.kt app/src/main/java/github/boxiaolanya2008/lingxihook/hook/HookRegistry.kt README.md
git commit -m "lxhook: 鏂板 deepopt 绌哄垪琛ㄦ嫤鎴?HookRegistry.kt:11 + IqooPowerSavingHook"
# 鎴栧甫 scope
# git commit -m "lxhook(powersaving): 鏂板 deepopt startDexoptJob 寮哄埗 false"
```

鍓嶇紑鍥哄畾 `lxhook:` 鎴?`lxhook(scope):`锛岃瑙?`COMMIT_CONVENTION.md`锛屼竴娆℃彁浜ゅ彧鍋氫竴浠朵簨銆?

### 5. 鍚堝苟鍥?master

鎺ㄨ崘 PR 鏂瑰紡锛堜繚鐣欏鏌ョ棔杩癸級锛?

```bash
git push -u origin feat/浣犵殑闇€姹?20260831
gh pr create --base master --title "lxhook: 鏂板 xxx" --body "鏀瑰姩锛歺xx锛涢獙璇侊細./gradlew :app:assembleDebug + 鐪熸満 V2520A installDebug 鏃ュ織 [deepopt] / [wireless] 姝ｅ父"
gh pr view --web
# CI 閫氳繃銆佸彟涓€妯″瀷鎴栦汉鐐?Approve 鍚?
gh pr merge --squash --delete-branch
git checkout master && git pull --ff-only origin master
```

鏈湴鐩存帴鍚堝苟涓嶈蛋杩滅锛堜粎绂荤嚎鏃剁敤锛夛細

```bash
git checkout master
git merge --no-ff feat/浣犵殑闇€姹?20260831 -m "merge feat/浣犵殑闇€姹?20260831 into master"
git branch -d feat/浣犵殑闇€姹?20260831
```

鍚堝苟鍚庣珛鍗?`./gradlew :app:installDebug` 鍦?`V2520A` 澶嶆祴銆岃缃啋璋冭壊鏉块鏍笺€嶆湁鏃犲疄闄呭彉鑹层€佹棩蹇楅〉绛涢€夋槸鍚︽甯搞€佹繁搴︿紭鍖栨寜閽槸鍚︾偣鍚庢棤鍔ㄤ綔銆?

## 鐩綍

```
app/src/main/java/.../hook/LingXiHook.kt        妯″潡鍏ュ彛锛屾寜鍖呭悕鍒嗗彂
app/src/main/java/.../hook/HookRegistry.kt:15   娉ㄥ唽琛紝棣栭〉鏁版嵁婧愶紝鏂板閫傞厤鍔犱竴琛?
app/src/main/java/.../hook/powersaving/         鐪佺數绠＄悊 Hook锛堟棤绾垮厖鐢?娣卞害浼樺寲/璇︽儏鍗★級lingxi_hook_wireless/deepopt/power_battery_card
app/src/main/java/.../hook/camera/              鐩告満 Hook锛圸EISS 姘村嵃 + 鍥炬爣鍏ㄦ樉 + 鏍″洯姘村嵃 + 楂樺儚绱狅級lingxi_hook_camera_*
app/src/main/java/.../hook/device/              鐪熷疄鐢甸噺锛坰ystem_server BatteryService 娲惧彂淇锛塴ingxi_hook_real_battery/_block_update
app/src/main/java/.../hook/abe/                 鏅烘収寮曟搸 ABE 闈欓粯閲嶅惎灞忚斀锛圫ilentRebootService#p0/o0/g0/v0 + PowerManager.reboot锛塴ingxi_hook_abe_silent_reboot
app/src/main/java/.../hook/fuelsummary/         鐢垫簮淇℃伅 FuelSummary 鍏呯數闄愭祦绉婚櫎/瀹归噺閿佹渶澶?寰幆閿?锛坮0.f/h + battery.health.a/g锛塴ingxi_hook_fuel_*
app/src/main/java/.../hook/space/               vivo 绀惧尯搴曢儴瀵艰埅鐜荤拑鍖栵紙SpaceVBottomNavigationView 鏋勯€犲櫒鎷︽埅锛? 骞垮憡灞忚斀锛堝紑灞忔诞灞?+ 鎺ㄨ崘娴佸箍鍛婃í鏉★級lingxi_hook_space_glass_nav/_adblock
app/src/main/java/.../hook/gamecube/             娓告垙榄旂洅瓒呭垎/鎻掑抚閰嶇疆璺緞閲嶅畾鍚戯紙FileInputStream/FileReader 鏋勯€犲櫒鎸夋枃浠跺悕鎷︽埅锛? Q鑺墖涓撳尯鍏ュ彛/鏄剧ず璁剧疆/娓告垙鍏夎拷/TAA 寮哄埗寮€鍚紙e0#h0 鎬婚椄涓庣‖缂栫爜鍚嶅崟鎷︽埅锛塴ingxi_hook_gamecube_config/_qzone/_display/_lighttrack/_taa
app/src/main/java/.../hook/update/              杩滅▼鏇存柊 UpdateInfo/Checker/Dialog锛堟棤缂撳瓨锛宖orce_update 鎺у脊绐楋級
app/src/main/java/.../util/RootUtil.kt          Root 妫€娴?su -c id
app/src/main/java/.../ui/theme/ColorScheme.kt   鍔ㄦ€佸彇鑹?+ spring 娓愬彉
app/src/main/java/.../ui/component/             SegmentedColumn / ExpressiveSwitch / G2Shapes
app/src/main/java/.../ui/component/liquidglass/ 娑叉€佺幓鐠冨簳閮ㄥ鑸紙鎶樺皠 Lens + miuix-blur backdrop + 闃诲凹鎷栧姩锛夛紝AppRoot.kt 鎺ュ叆
app/src/main/java/.../ui/component/miuix/       闃诲凹鎷栧姩鍔ㄧ敾 DampedDragAnimation / InteractiveHighlight / inspectDragGestures
update.json                                     杩滅▼鏇存柊鎻忚堪 5 鏍囪瘑
```

## 甯歌鍧?

- 鏀圭被鍚?鍖呭悕蹇呴』鍚屾 `resources/META-INF/xposed/java_init.list` 涓?`keepRules/rules.keep`锛屽惁鍒欓潤榛樹笉娉ㄥ叆銆?
- `scope.list` 涓?`arrays.xml` 鍙屼唤浣滅敤鍩熸敼涓€澶勬紡涓€澶勪細瀵艰嚧閮ㄥ垎妗嗘灦涓嶇敓鏁堛€?
- 鐩爣杩涚▼璇讳笉鍒?`SharedPreferences`锛屽紑鍏冲繀椤婚暅鍍忓埌 `Settings.System`銆?

## 璁稿彲璇?

鑷畾涔夊紑婧愯鍙瘉锛岃 [`LICENSE`](./LICENSE)銆傛牳蹇冧笁鏉★細鐢ㄥ氨蹇呴』甯﹀師浠撳簱 `github.boxiaolanya2008/LXHook` 涓庡師浣滆€?`boxiaolanya2008`锛涚姝㈠€掑崠锛涜繚鑰呮寜娆¤禂 50 涓囥€?
