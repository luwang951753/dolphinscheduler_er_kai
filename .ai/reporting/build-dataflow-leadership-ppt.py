#!/usr/bin/env python3
from __future__ import annotations

import html
import os
import shutil
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable
from xml.sax.saxutils import escape

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / ".ai" / "reporting"
SMOKE_DIR = ROOT / ".ai" / "qa" / "dataflow-module-smoke"
PPTX_PATH = OUT_DIR / "dataflow-dolphinscheduler-leadership-report-20260610.pptx"
MD_PATH = OUT_DIR / "dataflow-dolphinscheduler-leadership-report-20260610.md"
MEDIA_DIR = OUT_DIR / "_ppt_media"
REPORT_DATE = "2026-06-10"

EMU_PER_INCH = 914400
SLIDE_W = 13.333333
SLIDE_H = 7.5
W = int(SLIDE_W * EMU_PER_INCH)
H = int(SLIDE_H * EMU_PER_INCH)


def emu(value: float) -> int:
    return int(value * EMU_PER_INCH)


def tx(x: float, y: float, w: float, h: float, text: str, size: int = 24,
       color: str = "1F2937", bold: bool = False, font: str = "PingFang SC",
       align: str = "l") -> str:
    lines = text.split("\n")
    paras = []
    for line in lines:
        safe = escape(line)
        paras.append(
            f'<a:p><a:pPr algn="{align}"/>'
            f'<a:r><a:rPr lang="zh-CN" sz="{size * 100}" dirty="0" smtClean="0">'
            f'<a:solidFill><a:srgbClr val="{color}"/></a:solidFill>'
            f'<a:latin typeface="{font}"/><a:ea typeface="{font}"/>'
            f'{"<a:b/>" if bold else ""}</a:rPr><a:t>{safe}</a:t></a:r></a:p>'
        )
    body = "".join(paras)
    return (
        '<p:sp><p:nvSpPr><p:cNvPr id="1" name="Text Box"/>'
        '<p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr><p:spPr>'
        f'<a:xfrm><a:off x="{emu(x)}" y="{emu(y)}"/><a:ext cx="{emu(w)}" cy="{emu(h)}"/></a:xfrm>'
        '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom><a:noFill/></p:spPr>'
        '<p:txBody><a:bodyPr wrap="square" anchor="t"/><a:lstStyle/>'
        f'{body}</p:txBody></p:sp>'
    )


def rect(x: float, y: float, w: float, h: float, fill: str,
         stroke: str | None = None, radius: str = "roundRect") -> str:
    stroke_xml = '<a:ln><a:noFill/></a:ln>' if stroke is None else (
        f'<a:ln w="9525"><a:solidFill><a:srgbClr val="{stroke}"/></a:solidFill></a:ln>'
    )
    return (
        '<p:sp><p:nvSpPr><p:cNvPr id="2" name="Shape"/>'
        '<p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr>'
        f'<a:xfrm><a:off x="{emu(x)}" y="{emu(y)}"/><a:ext cx="{emu(w)}" cy="{emu(h)}"/></a:xfrm>'
        f'<a:prstGeom prst="{radius}"><a:avLst/></a:prstGeom>'
        f'<a:solidFill><a:srgbClr val="{fill}"/></a:solidFill>{stroke_xml}'
        '</p:spPr></p:sp>'
    )


def line(x1: float, y1: float, x2: float, y2: float, color: str = "CBD5E1") -> str:
    return (
        '<p:cxnSp><p:nvCxnSpPr><p:cNvPr id="3" name="Connector"/>'
        '<p:cNvCxnSpPr/><p:nvPr/></p:nvCxnSpPr><p:spPr>'
        f'<a:xfrm><a:off x="{emu(min(x1, x2))}" y="{emu(min(y1, y2))}"/>'
        f'<a:ext cx="{emu(abs(x2 - x1))}" cy="{emu(abs(y2 - y1))}"/></a:xfrm>'
        '<a:prstGeom prst="line"><a:avLst/></a:prstGeom>'
        f'<a:ln w="19050"><a:solidFill><a:srgbClr val="{color}"/></a:solidFill></a:ln>'
        '</p:spPr></p:cxnSp>'
    )


def rels_for_images(images: list[str]) -> str:
    rels = [
        '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>'
    ]
    for idx, image in enumerate(images, start=2):
        rels.append(
            f'<Relationship Id="rId{idx}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/{image}"/>'
        )
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        + "".join(rels) + '</Relationships>'
    )


@dataclass
class Slide:
    title: str
    subtitle: str = ""
    bullets: list[str] = field(default_factory=list)
    image: str | None = None
    image_title: str | None = None
    kind: str = "standard"


def copy_image(path: Path, index: int) -> tuple[str, tuple[int, int]]:
    MEDIA_DIR.mkdir(parents=True, exist_ok=True)
    img = Image.open(path)
    target = f"image{index}.png"
    shutil.copyfile(path, MEDIA_DIR / target)
    return target, img.size


def picture_xml(rel_id: str, name: str, x: float, y: float, w: float, h: float) -> str:
    return (
        '<p:pic><p:nvPicPr><p:cNvPr id="10" name="' + escape(name) + '"/>'
        '<p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr><p:nvPr/></p:nvPicPr>'
        '<p:blipFill><a:blip r:embed="' + rel_id + '"/>'
        '<a:stretch><a:fillRect/></a:stretch></p:blipFill><p:spPr>'
        f'<a:xfrm><a:off x="{emu(x)}" y="{emu(y)}"/><a:ext cx="{emu(w)}" cy="{emu(h)}"/></a:xfrm>'
        '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom>'
        '<a:ln w="6350"><a:solidFill><a:srgbClr val="CBD5E1"/></a:solidFill></a:ln>'
        '</p:spPr></p:pic>'
    )


def fit_image_box(px_w: int, px_h: int, box_x: float, box_y: float, box_w: float, box_h: float):
    ratio = px_w / px_h
    box_ratio = box_w / box_h
    if ratio >= box_ratio:
        w = box_w
        h = box_w / ratio
    else:
        h = box_h
        w = box_h * ratio
    return box_x + (box_w - w) / 2, box_y + (box_h - h) / 2, w, h


def bullet_list(x: float, y: float, bullets: Iterable[str], width: float = 5.2) -> str:
    parts = []
    yy = y
    palette = ["2563EB", "059669", "D97706", "7C3AED", "0891B2"]
    for i, b in enumerate(bullets):
        parts.append(rect(x, yy + 0.06, 0.12, 0.12, palette[i % len(palette)], None, "ellipse"))
        parts.append(tx(x + 0.28, yy, width, 0.42, b, size=16, color="334155"))
        yy += 0.48
    return "".join(parts)


def slide_xml(slide: Slide, slide_idx: int, image_info: tuple[str, tuple[int, int]] | None) -> tuple[str, list[str]]:
    shapes = []
    shapes.append(rect(0, 0, SLIDE_W, SLIDE_H, "F8FAFC", None, "rect"))
    shapes.append(rect(0, 0, 0.12, SLIDE_H, "2563EB", None, "rect"))

    if slide.kind == "cover":
        shapes.append(tx(0.72, 0.78, 10.8, 0.9, slide.title, size=34, bold=True, color="0F172A"))
        shapes.append(tx(0.76, 1.78, 10.2, 0.6, slide.subtitle, size=18, color="475569"))
        shapes.append(rect(0.76, 2.72, 3.0, 0.72, "DBEAFE", "BFDBFE"))
        shapes.append(tx(0.98, 2.9, 2.6, 0.3, "可演示", size=18, bold=True, color="1D4ED8", align="c"))
        shapes.append(rect(4.05, 2.72, 3.0, 0.72, "DCFCE7", "BBF7D0"))
        shapes.append(tx(4.27, 2.9, 2.6, 0.3, "可交付", size=18, bold=True, color="047857", align="c"))
        shapes.append(rect(7.34, 2.72, 3.0, 0.72, "FEF3C7", "FDE68A"))
        shapes.append(tx(7.56, 2.9, 2.6, 0.3, "可部署", size=18, bold=True, color="B45309", align="c"))
        shapes.append(tx(0.78, 5.9, 8.8, 0.4, "基于 DolphinScheduler 二开建设轻量数据中台 DataFlow", size=20, color="334155"))
        shapes.append(tx(0.78, 6.42, 8.8, 0.3, f"汇报日期：{REPORT_DATE}", size=14, color="64748B"))
    elif slide.kind == "architecture":
        shapes.append(tx(0.55, 0.35, 11.8, 0.5, slide.title, size=26, bold=True, color="0F172A"))
        labels = ["数据源", "同步任务", "数据预览", "主题库/治理", "Magic API", "回传/下发", "白皮书"]
        colors = ["E0F2FE", "DBEAFE", "DCFCE7", "FEF3C7", "F3E8FF", "FFE4E6", "E2E8F0"]
        xs = [0.65, 2.35, 4.05, 5.75, 7.45, 9.15, 10.85]
        for i, label in enumerate(labels):
            shapes.append(rect(xs[i], 2.45, 1.32, 0.78, colors[i], "CBD5E1"))
            shapes.append(tx(xs[i], 2.68, 1.32, 0.28, label, size=14, bold=True, color="0F172A", align="c"))
            if i < len(labels) - 1:
                shapes.append(line(xs[i] + 1.36, 2.84, xs[i + 1] - 0.05, 2.84, "94A3B8"))
        shapes.append(bullet_list(1.0, 4.08, slide.bullets, width=10.4))
    elif slide.kind == "summary":
        shapes.append(tx(0.55, 0.35, 11.8, 0.5, slide.title, size=26, bold=True, color="0F172A"))
        cards = [
            ("数据接入与调度", "同步任务复用 DolphinScheduler 工作流、定时、上线下线和执行日志。", "DBEAFE"),
            ("数据理解与治理", "数据预览、主题库、治理、SQL 血缘形成可解释的数据使用链路。", "DCFCE7"),
            ("业务服务输出", "Magic API 承载接口开发，支撑首页、回传、下发等业务查询。", "F3E8FF"),
            ("报告与交付", "白皮书模块支持可编辑报告、报表数据集、图表/表格嵌入。", "FEF3C7"),
        ]
        for i, (h, body, c) in enumerate(cards):
            x = 0.78 + (i % 2) * 6.05
            y = 1.45 + (i // 2) * 2.35
            shapes.append(rect(x, y, 5.35, 1.65, c, "CBD5E1"))
            shapes.append(tx(x + 0.3, y + 0.28, 4.8, 0.28, h, size=18, bold=True, color="0F172A"))
            shapes.append(tx(x + 0.3, y + 0.72, 4.65, 0.6, body, size=14, color="334155"))
    else:
        shapes.append(tx(0.55, 0.32, 11.8, 0.5, slide.title, size=25, bold=True, color="0F172A"))
        if slide.subtitle:
            shapes.append(tx(0.58, 0.86, 11.4, 0.35, slide.subtitle, size=14, color="64748B"))
        if image_info:
            img_name, (px_w, px_h) = image_info
            x, y, w, h = fit_image_box(px_w, px_h, 6.35, 1.35, 6.35, 4.88)
            shapes.append(rect(6.18, 1.18, 6.72, 5.22, "FFFFFF", "E2E8F0"))
            shapes.append(picture_xml("rId2", img_name, x, y, w, h))
            if slide.image_title:
                shapes.append(tx(6.24, 6.52, 6.3, 0.26, slide.image_title, size=12, color="64748B", align="c"))
            shapes.append(bullet_list(0.78, 1.52, slide.bullets, width=5.05))
        else:
            shapes.append(bullet_list(1.0, 1.35, slide.bullets, width=10.5))

    xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" '
        'xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">'
        '<p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/>'
        '<p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr>'
        '<a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm>'
        '</p:grpSpPr>' + "".join(shapes) + '</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>'
    )
    images = [image_info[0]] if image_info else []
    return xml, images


def content_types_xml(count: int) -> str:
    slide_overrides = "".join(
        f'<Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>'
        for i in range(1, count + 1)
    )
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        '<Default Extension="xml" ContentType="application/xml"/>'
        '<Default Extension="png" ContentType="image/png"/>'
        '<Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>'
        '<Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>'
        '<Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>'
        '<Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>'
        f'{slide_overrides}</Types>'
    )


def static_file(name: str) -> str:
    files = {
        "_rels/.rels": (
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>'
            '</Relationships>'
        ),
        "ppt/slideLayouts/slideLayout1.xml": (
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            '<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1">'
            '<p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>'
        ),
        "ppt/slideLayouts/_rels/slideLayout1.xml.rels": (
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>'
            '</Relationships>'
        ),
        "ppt/slideMasters/slideMaster1.xml": (
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            '<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">'
            '<p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld>'
            '<p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>'
            '<p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst></p:sldMaster>'
        ),
        "ppt/slideMasters/_rels/slideMaster1.xml.rels": (
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>'
            '<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>'
            '</Relationships>'
        ),
        "ppt/theme/theme1.xml": (
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            '<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="DataFlow">'
            '<a:themeElements><a:clrScheme name="DataFlow"><a:dk1><a:srgbClr val="0F172A"/></a:dk1><a:lt1><a:srgbClr val="FFFFFF"/></a:lt1><a:dk2><a:srgbClr val="334155"/></a:dk2><a:lt2><a:srgbClr val="F8FAFC"/></a:lt2><a:accent1><a:srgbClr val="2563EB"/></a:accent1><a:accent2><a:srgbClr val="059669"/></a:accent2><a:accent3><a:srgbClr val="D97706"/></a:accent3><a:accent4><a:srgbClr val="7C3AED"/></a:accent4><a:accent5><a:srgbClr val="0891B2"/></a:accent5><a:accent6><a:srgbClr val="E11D48"/></a:accent6><a:hlink><a:srgbClr val="2563EB"/></a:hlink><a:folHlink><a:srgbClr val="7C3AED"/></a:folHlink></a:clrScheme>'
            '<a:fontScheme name="DataFlow"><a:majorFont><a:latin typeface="PingFang SC"/><a:ea typeface="PingFang SC"/></a:majorFont><a:minorFont><a:latin typeface="PingFang SC"/><a:ea typeface="PingFang SC"/></a:minorFont></a:fontScheme><a:fmtScheme name="DataFlow"><a:fillStyleLst/><a:lnStyleLst/><a:effectStyleLst/><a:bgFillStyleLst/></a:fmtScheme></a:themeElements></a:theme>'
        ),
    }
    return files[name]


def presentation_xml(count: int) -> str:
    ids = "".join(f'<p:sldId id="{256 + i}" r:id="rId{i}"/>' for i in range(1, count + 1))
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">'
        f'<p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId{count + 1}"/></p:sldMasterIdLst><p:sldIdLst>{ids}</p:sldIdLst>'
        f'<p:sldSz cx="{W}" cy="{H}" type="wide"/><p:notesSz cx="{emu(10)}" cy="{emu(7.5)}"/></p:presentation>'
    )


def presentation_rels(count: int) -> str:
    rels = [
        f'<Relationship Id="rId{i}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide{i}.xml"/>'
        for i in range(1, count + 1)
    ]
    rels.append(
        f'<Relationship Id="rId{count + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>'
    )
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        + "".join(rels) + '</Relationships>'
    )


def build_slides() -> list[Slide]:
    return [
        Slide("DataFlow DolphinScheduler 二开阶段成果汇报", "面向领导汇报：建设目标、业务编排、模块能力、验证结果与下一步计划", kind="cover"),
        Slide("一、建设定位", bullets=[
            "以 DolphinScheduler 为底座，形成轻量数据中台 DataFlow。",
            "目标不是单点原型，而是可演示、可交付、可部署的业务系统。",
            "围绕公安业务数据链路，覆盖接入、处理、服务、回传、报告和安全运维。",
            "采用 ECC 工作方式推进：小步实现、接口清楚、验证闭环。"
        ]),
        Slide("二、总体业务链路", kind="architecture", bullets=[
            "数据从业务库/外部库进入平台，经同步任务调度进入中台数据区。",
            "平台提供数据预览、主题库、治理和血缘，支撑数据可理解、可追溯。",
            "Magic API 作为业务接口开发层，向首页指标、回传、下发等模块供数。",
            "白皮书把可配置报表嵌入可编辑文档，形成面向汇报和交付的输出物。"
        ]),
        Slide("三、当前模块编排", kind="summary"),
        Slide("首页：数据中台驾驶舱", "统一呈现平台运行态势和关键业务指标。", [
            "展示同步任务、主题库、治理、回传下发、接口服务等核心指标。",
            "定位为领导和运维人员的第一入口，用于快速判断平台运行状态。",
            "后续指标应优先通过 Magic API 或明确后端接口获取，避免静态假数据。"
        ], "01-首页.png", "首页模块实际界面截图"),
        Slide("同步任务：数据接入与调度执行", "复用 DolphinScheduler 工作流能力，承载 DataFlow 同步业务。", [
            "支持同步任务列表、新建向导、字段映射、过滤条件、DDL 与运行配置。",
            "右侧操作风格与项目工作流定义保持一致，可复用定时、上线下线、执行记录。",
            "SeaTunnel 定位为同步执行组件，DolphinScheduler 负责流程编排和调度。"
        ], "04-同步任务.png", "同步任务模块实际界面截图"),
        Slide("数据预览：快速看表、看字段、看样例", "让维护人员在不写复杂 SQL 的情况下判断数据是否可用。", [
            "支持数据源、库表、字段和条件筛选，提供数据预览与字段操作体验。",
            "面向 MySQL 与 Oracle 等数据源兼容，后续可接入 profile 指标。",
            "是后续“数据探查 + 找数找字段”的基础交互参考。"
        ], "05-数据预览.png", "数据预览模块实际界面截图"),
        Slide("主题库：面向业务的数据组织", "把平台数据按公安业务主题组织起来，而不是只暴露技术表。", [
            "围绕人员、案件、警情、线索等业务视角沉淀主题数据。",
            "与首页指标、数据治理、Magic API 形成上下游关系。",
            "后续重点是把主题数据来源、更新频率、口径说明做清楚。"
        ], "06-主题库.png", "主题库模块实际界面截图"),
        Slide("数据治理 + SQL 血缘：质量与可追溯", "补齐数据质量、问题发现和 SQL 表/字段级血缘能力。", [
            "数据治理模块展示质量概览、问题项、同步链路和治理状态。",
            "SQL 血缘解析支持在线输入 SQL，展示表级/字段级血缘图。",
            "第一版以 JSqlParser + G6 跑通 MVP，复杂方言后续再逐步增强。"
        ], "08-SQL-血缘解析.png", "SQL 血缘解析实际界面截图"),
        Slide("Magic API：业务接口开发层", "作为 DataFlow 模块对外和对内供数的重要接口层。", [
            "嵌入 Dolphin 二开项目内，用于开发、维护、演示业务查询接口。",
            "计划承接首页指标、主题库查询、数据回传、数据下发等接口。",
            "接口路径和分组需要沉淀文档，方便交付后维护和现场演示。"
        ], "30-Magic-API.png", "Magic API 模块实际界面截图"),
        Slide("数据回传 / 数据下发：平台处理结果回到业务侧", "表达数据中台处理结果向业务系统或目标库输出的链路。", [
            "数据回传支持嫌疑人编号、姓名、身份证号、案件编号、案件名称检索。",
            "回传库 Tab 包括天地伟业中间库、市局 C3 回传库、警综库。",
            "数据下发采用相同筛选口径，Tab 包括数据中台、三期平台。",
            "具体查询内容对接 Magic API，保证展示内容可解释、可维护。"
        ], "09-数据回传.png", "数据回传模块实际界面截图"),
        Slide("白皮书：可编辑报告与可配置报表", "把平台数据、统计图表和文字说明沉淀为可汇报材料。", [
            "支持模板编辑器、报表数据集、预览导出三个核心工作区。",
            "报表数据集可配置字段、条件、维度、指标和展现形式。",
            "图表、饼图、表格等可作为展现形式嵌入文档指定位置。",
            "后续可引入成熟富文本组件，提升 Word 类编辑体验。"
        ], "11-白皮书.png", "白皮书模块实际界面截图"),
        Slide("资源、监控、安全：交付运行底座", "保留 DolphinScheduler 运维能力，并按 DataFlow 导航重新编排。", [
            "资源模块归入监控体系，白皮书进入业务模块位置。",
            "监控覆盖实例统计、Master、Worker、Alert、DB、审计日志等。",
            "安全中心覆盖租户、用户、令牌、环境、集群、告警等管理。",
            "权限边界需区分 admin、维护人员、只读用户，不能只做前端隐藏。"
        ], "13-监控-实例统计.png", "监控模块实际界面截图"),
        Slide("安装与验证结果", "已基于二开项目完成打包、安装和浏览器冒烟验证。", [
            "打包产物：apache-dolphinscheduler-3.4.1-bin.tar.gz。",
            "安装验证使用 MySQL 数据库，不使用 H2。",
            "服务健康检查：api、db、master、worker、alert 均为 UP。",
            "浏览器模拟点击 30 个模块，结果 30/30 PASS，控制台错误 0，失败响应 0。"
        ], "report.md", "冒烟验证报告摘要"),
        Slide("下一步工作计划", "围绕“可交付”继续补齐真实接口、权限和演示数据闭环。", [
            "补齐 Magic API 接口：优先首页指标、回传、下发、主题库查询。",
            "完善数据探查：搜索表/字段、Profile 指标、样例值、脱敏预览。",
            "深化白皮书：成熟富文本、报表数据集合法性校验、导出能力。",
            "强化权限与安全：SQL、数据源、导出、回传、接口调用做后端校验。",
            "继续保持打包安装和浏览器点击验证，形成交付前检查清单。"
        ]),
    ]


def write_markdown(slides: list[Slide]) -> None:
    lines = ["# DataFlow DolphinScheduler 二开阶段成果汇报", "", "汇报日期：2026-06-10", ""]
    for idx, slide in enumerate(slides, start=1):
        lines.append(f"## {idx}. {slide.title}")
        if slide.subtitle:
            lines.append("")
            lines.append(slide.subtitle)
        if slide.bullets:
            lines.append("")
            for b in slide.bullets:
                lines.append(f"- {b}")
        if slide.image:
            lines.append("")
            lines.append(f"配图：`.ai/qa/dataflow-module-smoke/{slide.image}`")
        lines.append("")
    MD_PATH.write_text("\n".join(lines), encoding="utf-8")


def build_pptx() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    if MEDIA_DIR.exists():
        shutil.rmtree(MEDIA_DIR)
    MEDIA_DIR.mkdir(parents=True, exist_ok=True)

    slides = build_slides()
    write_markdown(slides)

    slide_xmls: list[tuple[str, list[str]]] = []
    media_index = 1
    for slide in slides:
        image_info = None
        if slide.image and slide.image.endswith(".png"):
            img_name, size = copy_image(SMOKE_DIR / slide.image, media_index)
            media_index += 1
            image_info = (img_name, size)
        elif slide.image == "report.md":
            # Render report evidence as text-only slide; no image relation needed.
            image_info = None
        slide_xmls.append(slide_xml(slide, len(slide_xmls) + 1, image_info))

    with zipfile.ZipFile(PPTX_PATH, "w", compression=zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types_xml(len(slides)))
        for name in [
            "_rels/.rels", "ppt/slideLayouts/slideLayout1.xml",
            "ppt/slideLayouts/_rels/slideLayout1.xml.rels", "ppt/slideMasters/slideMaster1.xml",
            "ppt/slideMasters/_rels/slideMaster1.xml.rels", "ppt/theme/theme1.xml",
        ]:
            z.writestr(name, static_file(name))
        z.writestr("ppt/presentation.xml", presentation_xml(len(slides)))
        z.writestr("ppt/_rels/presentation.xml.rels", presentation_rels(len(slides)))
        for idx, (xml, images) in enumerate(slide_xmls, start=1):
            z.writestr(f"ppt/slides/slide{idx}.xml", xml)
            z.writestr(f"ppt/slides/_rels/slide{idx}.xml.rels", rels_for_images(images))
        for media in sorted(MEDIA_DIR.glob("*.png")):
            z.write(media, f"ppt/media/{media.name}")

    print(PPTX_PATH)
    print(MD_PATH)
    print(f"slides={len(slides)}")


if __name__ == "__main__":
    build_pptx()
