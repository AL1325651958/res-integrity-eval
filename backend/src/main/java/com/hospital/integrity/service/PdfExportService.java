package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.integrity.entity.ResearchAchievement;
import com.hospital.integrity.entity.ResearchIntegrity;
import com.hospital.integrity.entity.ResearchIntegrityDetail;
import com.hospital.integrity.entity.SysUser;
import com.hospital.integrity.mapper.ResearchAchievementMapper;
import com.hospital.integrity.mapper.ResearchIntegrityDetailMapper;
import com.hospital.integrity.mapper.SysUserMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * 个人科研诚信档案 PDF 导出（OpenPDF，中文字体：Windows 宋体 / Linux 文泉驿）
 */
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final SysUserMapper sysUserMapper;
    private final ResearchAchievementMapper achievementMapper;
    private final ResearchIntegrityDetailMapper detailMapper;

    public void exportArchive(Long userId, Integer year, HttpServletResponse response) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new com.hospital.integrity.common.BusinessException("用户不存在");
        }
        int y = year == null ? LocalDate.now().getYear() : year;
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=archive_" + user.getEmpNo() + "_" + y + ".pdf");
        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = BaseFont.createFont(fontPath(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font normalFont = new Font(baseFont, 10, Font.NORMAL);
            Font headFont = new Font(baseFont, 12, Font.BOLD);

            document.add(new Paragraph("个人科研诚信档案", titleFont));
            document.add(new Paragraph("姓名：" + user.getRealName() + "　工号：" + user.getEmpNo()
                    + "　评价年度：" + y, normalFont));
            document.add(new Paragraph(" "));

            // 成果清单
            document.add(new Paragraph("一、年度科研成果", headFont));
            List<ResearchAchievement> achs = achievementMapper.selectList(
                    new LambdaQueryWrapper<ResearchAchievement>()
                            .eq(ResearchAchievement::getUserId, userId)
                            .eq(ResearchAchievement::getStatus, 3)
                            .apply("YEAR(publish_time) = {0}", y));
            PdfPTable achTable = new PdfPTable(5);
            achTable.setWidths(new float[]{4, 2, 2, 2, 1});
            addCell(achTable, "成果名称", headFont);
            addCell(achTable, "类型", headFont);
            addCell(achTable, "级别", headFont);
            addCell(achTable, "位次", headFont);
            addCell(achTable, "得分", headFont);
            for (ResearchAchievement a : achs) {
                addCell(achTable, a.getTitle(), normalFont);
                addCell(achTable, a.getAchType(), normalFont);
                addCell(achTable, a.getLevel(), normalFont);
                addCell(achTable, a.getRankInfo(), normalFont);
                addCell(achTable, String.valueOf(a.getScore()), normalFont);
            }
            document.add(achTable);
            document.add(new Paragraph(" "));

            // 评价结论
            document.add(new Paragraph("二、年度评价结论", headFont));
            List<ResearchIntegrityDetail> details = detailMapper.selectList(
                    new LambdaQueryWrapper<ResearchIntegrityDetail>()
                            .eq(ResearchIntegrityDetail::getUserId, userId)
                            .eq(ResearchIntegrityDetail::getYear, y));
            PdfPTable detailTable = new PdfPTable(3);
            detailTable.setWidths(new float[]{3, 1, 1});
            addCell(detailTable, "计分明细", headFont);
            addCell(detailTable, "类型", headFont);
            addCell(detailTable, "分值", headFont);
            for (ResearchIntegrityDetail d : details) {
                addCell(detailTable, d.getItemName(), normalFont);
                addCell(detailTable, "PERF".equals(d.getBizType()) ? "业绩" : "扣分", normalFont);
                addCell(detailTable, String.valueOf(d.getScore()), normalFont);
            }
            document.add(detailTable);

            document.close();
        } catch (Exception e) {
            throw new com.hospital.integrity.common.BusinessException("档案导出失败：" + e.getMessage());
        }
    }

    private void addCell(PdfPTable table, String text, Font font) {
        table.addCell(new com.lowagie.text.Phrase(text == null ? "" : text, font));
    }

    private String fontPath() {
        // 优先 Windows 宋体，其次 Linux 文泉驿/思源黑体
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "C:/Windows/Fonts/simsun.ttc";
        }
        String[] candidates = {
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/arphic/uming.ttc"
        };
        for (String c : candidates) {
            if (java.nio.file.Files.exists(java.nio.file.Paths.get(c))) {
                return c;
            }
        }
        return candidates[0];
    }
}
