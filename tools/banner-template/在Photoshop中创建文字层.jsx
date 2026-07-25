/**
 * Photoshop 脚本：打开干净背景，创建可真正改字的文字层
 *
 * 用法：
 * 1. Photoshop → 文件 → 脚本 → 浏览 → 选本文件
 * 2. 若提示选背景图，选择同目录 background.png / background-@2x.png
 * 3. 图层面板会出现「主标题 / 副标题1 / 副标题2 / 域名」文字层，直接改字即可
 * 4. 文件 → 导出 → 存储为 Web 所用格式 → GIF，可再自己加帧做动画
 *    或把单帧导出后，用 make_gif.py 做扫光动画
 */

#target photoshop
app.bringToFront();

function main() {
  var scriptFile = new File($.fileName);
  var folder = scriptFile.parent;
  // 优先：你用内容识别/生成式填充去字后的底图；否则用 @2x 起始帧
  var bgFile = new File(folder + "/background-@2x.png");
  if (!bgFile.exists) bgFile = new File(folder + "/background.png");
  if (!bgFile.exists) bgFile = new File(folder + "/ps-起始帧@2x.png");
  if (!bgFile.exists) bgFile = new File(folder + "/ps-起始帧-请用内容识别填充去字.png");
  if (!bgFile.exists) {
    bgFile = File.openDialog("请选择去字后的横幅背景 PNG", "*.png");
  }
  if (!bgFile) return;

  var doc = app.open(bgFile);
  var w = doc.width.as("px");
  var h = doc.height.as("px");

  // 默认文案（改这里或在 PS 里直接改层）
  var title = "刘伯温";
  var sub1 = "帝皇之师刘伯温";
  var sub2 = "神机妙算 解密财富";
  var domain = "585520.xyz";

  addGoldText(doc, "主标题", title, w * 0.40, h * 0.28, h * 0.32, true);
  addGoldText(doc, "副标题1", sub1, w * 0.42, h * 0.55, h * 0.085, false);
  addGoldText(doc, "副标题2", sub2, w * 0.42, h * 0.66, h * 0.08, false);
  addGoldText(doc, "域名", domain, w * 0.50, h * 0.88, h * 0.145, false);

  alert("已创建可编辑文字层。\n直接在图层面板双击文字层改文案/域名即可。");
}

function addGoldText(doc, name, contents, x, y, sizePx, tracking) {
  var layer = doc.artLayers.add();
  layer.kind = LayerKind.TEXT;
  layer.name = name;

  var ti = layer.textItem;
  ti.kind = TextType.POINTTEXT;
  ti.contents = contents;
  ti.position = [UnitValue(x, "px"), UnitValue(y, "px")];
  ti.size = UnitValue(sizePx, "px");
  ti.justification = Justification.CENTER;
  ti.useAutoLeading = true;
  try {
    ti.font = "SongtiSC-Bold";
  } catch (e1) {
    try { ti.font = "STSong"; } catch (e2) {}
  }
  // 金色
  var c = new SolidColor();
  c.rgb.red = 255;
  c.rgb.green = 210;
  c.rgb.blue = 100;
  ti.color = c;

  if (tracking) {
    try { ti.tracking = 80; } catch (e3) {}
  }

  // 描边效果（图层样式）
  try {
    applyStroke(doc, layer, 90, 26, 8, Math.max(3, Math.round(sizePx / 14)));
  } catch (e4) {}
}

function applyStroke(doc, layer, r, g, b, size) {
  // 简化：有的 PS 版本图层样式描述符较复杂，失败则仅保留纯色字
  doc.activeLayer = layer;
  var desc = new ActionDescriptor();
  var ref = new ActionReference();
  ref.putProperty(charIDToTypeID("Prpr"), charIDToTypeID("Lefx"));
  ref.putEnumerated(charIDToTypeID("Lyr "), charIDToTypeID("Ordn"), charIDToTypeID("Trgt"));
  desc.putReference(charIDToTypeID("null"), ref);

  var fx = new ActionDescriptor();
  fx.putUnitDouble(charIDToTypeID("Scl "), charIDToTypeID("#Prc"), 100);

  var stroke = new ActionDescriptor();
  stroke.putBoolean(charIDToTypeID("enab"), true);
  stroke.putEnumerated(charIDToTypeID("Styl"), charIDToTypeID("FStl"), charIDToTypeID("OutF"));
  stroke.putEnumerated(charIDToTypeID("PntT"), charIDToTypeID("FrFl"), charIDToTypeID("SClr"));
  stroke.putEnumerated(charIDToTypeID("Md  "), charIDToTypeID("BlnM"), charIDToTypeID("Nrml"));
  stroke.putUnitDouble(charIDToTypeID("Opct"), charIDToTypeID("#Prc"), 100);
  stroke.putUnitDouble(charIDToTypeID("Sz  "), charIDToTypeID("#Pxl"), size);

  var color = new ActionDescriptor();
  color.putDouble(charIDToTypeID("Rd  "), r);
  color.putDouble(charIDToTypeID("Grn "), g);
  color.putDouble(charIDToTypeID("Bl  "), b);
  stroke.putObject(charIDToTypeID("Clr "), charIDToTypeID("RGBC"), color);

  fx.putObject(charIDToTypeID("FrFX"), charIDToTypeID("FrFX"), stroke);
  desc.putObject(charIDToTypeID("T   "), charIDToTypeID("Lefx"), fx);
  executeAction(charIDToTypeID("setd"), desc, DialogModes.NO);
}

main();
