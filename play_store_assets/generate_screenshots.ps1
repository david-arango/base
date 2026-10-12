param()

$csharpCode = @"
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;

public class PlayStoreScreenshotBuilder {
    private const int W = 1080;
    private const int H = 2400;

    public static GraphicsPath RoundedRect(RectangleF bounds, float radius) {
        float diameter = radius * 2;
        GraphicsPath path = new GraphicsPath();
        path.AddArc(bounds.X, bounds.Y, diameter, diameter, 180, 90);
        path.AddArc(bounds.Right - diameter, bounds.Y, diameter, diameter, 270, 90);
        path.AddArc(bounds.Right - diameter, bounds.Bottom - diameter, diameter, diameter, 0, 90);
        path.AddArc(bounds.X, bounds.Bottom - diameter, diameter, diameter, 90, 90);
        path.CloseFigure();
        return path;
    }

    private static void DrawPhoneFrame(Graphics g, float phoneX, float phoneY, float phoneW, float phoneH, bool isDark) {
        // Drop shadow
        Color shadowColor = isDark ? Color.FromArgb(40, 0, 0, 0) : Color.FromArgb(20, 0, 0, 0);
        using (SolidBrush sBrush = new SolidBrush(shadowColor)) {
            using (GraphicsPath sp = RoundedRect(new RectangleF(phoneX - 10, phoneY - 4, phoneW + 20, phoneH + 20), 66f)) {
                g.FillPath(sBrush, sp);
            }
        }

        // Phone body chassis
        Color bodyColor = isDark ? ColorTranslator.FromHtml("#282F2C") : ColorTranslator.FromHtml("#E4E2DC");
        Color borderColor = isDark ? ColorTranslator.FromHtml("#38423E") : ColorTranslator.FromHtml("#D0CEC7");
        using (SolidBrush bBrush = new SolidBrush(bodyColor))
        using (Pen bPen = new Pen(borderColor, 3f))
        using (GraphicsPath bp = RoundedRect(new RectangleF(phoneX, phoneY, phoneW, phoneH), 58f)) {
            g.FillPath(bBrush, bp);
            g.DrawPath(bPen, bp);
        }
    }

    private static void DrawPhoneStatusBar(Graphics g, float sx, float sy, float sw, bool isDark) {
        Color textColor = isDark ? ColorTranslator.FromHtml("#E0E3E1") : ColorTranslator.FromHtml("#181C1B");
        using (Font font = new Font("Segoe UI", 20f, FontStyle.Bold, GraphicsUnit.Pixel))
        using (SolidBrush brush = new SolidBrush(textColor)) {
            g.DrawString("9:41", font, brush, sx + 44f, sy + 18f);
            g.DrawString("100%", font, brush, sx + sw - 105f, sy + 18f);
        }

        // Camera notch pill
        float notchW = 120f;
        float notchH = 22f;
        float notchX = sx + (sw - notchW) / 2f;
        Color notchColor = isDark ? ColorTranslator.FromHtml("#1E2421") : ColorTranslator.FromHtml("#ECEAE4");
        using (SolidBrush nBrush = new SolidBrush(notchColor))
        using (GraphicsPath np = RoundedRect(new RectangleF(notchX, sy + 14f, notchW, notchH), 11f)) {
            g.FillPath(nBrush, np);
        }
    }

    private static void DrawPhoneNavBar(Graphics g, float sx, float sy, float sw, float sh, int activeTab, bool isDark) {
        float barH = 100f;
        float barY = sy + sh - barH;

        Color barBg = isDark ? ColorTranslator.FromHtml("#1A201E") : ColorTranslator.FromHtml("#FFFFFF");
        Color borderC = isDark ? ColorTranslator.FromHtml("#28302D") : ColorTranslator.FromHtml("#EAE9E4");
        Color activeC = isDark ? ColorTranslator.FromHtml("#6AA576") : ColorTranslator.FromHtml("#2B5A33");
        Color inactiveC = isDark ? ColorTranslator.FromHtml("#7C8681") : ColorTranslator.FromHtml("#848B87");

        using (SolidBrush bBrush = new SolidBrush(barBg))
        using (Pen bPen = new Pen(borderC, 1.5f)) {
            g.FillRectangle(bBrush, sx, barY, sw, barH);
            g.DrawLine(bPen, sx, barY, sx + sw, barY);
        }

        string[] tabs = new string[] { "Ledger", "Analytics", "Settings" };
        float tabW = sw / 3f;

        using (Font activeFont = new Font("Segoe UI", 21f, FontStyle.Bold, GraphicsUnit.Pixel))
        using (Font inactiveFont = new Font("Segoe UI", 20f, FontStyle.Regular, GraphicsUnit.Pixel))
        using (SolidBrush activeBrush = new SolidBrush(activeC))
        using (SolidBrush inactiveBrush = new SolidBrush(inactiveC)) {
            StringFormat sf = new StringFormat { Alignment = StringAlignment.Center };
            for (int i = 0; i < 3; i++) {
                float tx = sx + (i * tabW);
                bool isActive = (i == activeTab);
                Font f = isActive ? activeFont : inactiveFont;
                SolidBrush b = isActive ? activeBrush : inactiveBrush;

                // Tab icon indicator
                string icon = isActive ? "●" : "○";
                g.DrawString(icon, activeFont, b, new RectangleF(tx, barY + 12f, tabW, 30f), sf);
                g.DrawString(tabs[i], f, b, new RectangleF(tx, barY + 42f, tabW, 30f), sf);
            }
        }

        // Gesture handle
        float pillW = 160f;
        float pillH = 5f;
        float pillX = sx + (sw - pillW) / 2f;
        Color pillColor = isDark ? ColorTranslator.FromHtml("#4E5853") : ColorTranslator.FromHtml("#C8C6BF");
        using (SolidBrush pBrush = new SolidBrush(pillColor))
        using (GraphicsPath pp = RoundedRect(new RectangleF(pillX, sy + sh - 14f, pillW, pillH), 2.5f)) {
            g.FillPath(pBrush, pp);
        }
    }

    public static void GenerateScreenshot1(string path) {
        using (Bitmap bmp = new Bitmap(W, H, PixelFormat.Format24bppRgb))
        using (Graphics g = Graphics.FromImage(bmp)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            // Outer canvas
            using (SolidBrush bg = new SolidBrush(ColorTranslator.FromHtml("#F3F2EE"))) {
                g.FillRectangle(bg, 0, 0, W, H);
            }

            // Marketing Header
            using (Font tf = new Font("Segoe UI", 56f, FontStyle.Bold, GraphicsUnit.Pixel))
            using (Font sf = new Font("Segoe UI", 27f, FontStyle.Regular, GraphicsUnit.Pixel))
            using (SolidBrush tb = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
            using (SolidBrush sb = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                StringFormat fmt = new StringFormat { Alignment = StringAlignment.Center };
                g.DrawString("Perpetual Rolling Balance", tf, tb, new RectangleF(50, 110, W - 100, 80), fmt);
                g.DrawString("Real-time liquidity across time, without artificial monthly resets", sf, sb, new RectangleF(50, 195, W - 100, 100), fmt);
            }

            // Phone
            float pw = 880f, ph = 1890f, px = (W - pw) / 2f, py = 430f;
            DrawPhoneFrame(g, px, py, pw, ph, false);

            float pad = 18f, sx = px + pad, sy = py + pad, sw = pw - pad * 2, sh = ph - pad * 2;
            using (GraphicsPath sp = RoundedRect(new RectangleF(sx, sy, sw, sh), 42f)) {
                g.SetClip(sp);
                using (SolidBrush sbg = new SolidBrush(ColorTranslator.FromHtml("#F9F9F8"))) {
                    g.FillRectangle(sbg, sx, sy, sw, sh);
                }

                DrawPhoneStatusBar(g, sx, sy, sw, false);

                // App Header
                float topY = sy + 68f;
                using (SolidBrush lb = new SolidBrush(ColorTranslator.FromHtml("#3A5A40"))) {
                    g.FillEllipse(lb, sx + 36f, topY, 44f, 44f);
                }
                using (SolidBrush db = new SolidBrush(ColorTranslator.FromHtml("#A3B18A"))) {
                    g.FillEllipse(db, sx + 50f, topY + 9f, 16f, 16f);
                }
                using (Font hf = new Font("Segoe UI", 32f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font mf = new Font("Segoe UI", 22f, FontStyle.Regular, GraphicsUnit.Pixel))
                using (SolidBrush htb = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
                using (SolidBrush mtb = new SolidBrush(ColorTranslator.FromHtml("#848B87"))) {
                    g.DrawString("Base Ledger", hf, htb, sx + 96f, topY + 2f);
                    g.DrawString("October 2026", mf, mtb, sx + sw - 180f, topY + 8f);
                }

                // Balance Card
                float cx = sx + 32f, cy = topY + 74f, cw = sw - 64f, ch = 280f;
                using (GraphicsPath cp = RoundedRect(new RectangleF(cx, cy, cw, ch), 28f)) {
                    using (SolidBrush cbg = new SolidBrush(Color.White)) g.FillPath(cbg, cp);
                    using (Pen cpPen = new Pen(ColorTranslator.FromHtml("#E2E2DF"), 1.5f)) g.DrawPath(cpPen, cp);
                }

                using (Font capF = new Font("Segoe UI", 18f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font balF = new Font("Segoe UI", 68f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font subF = new Font("Segoe UI", 21f, FontStyle.Regular, GraphicsUnit.Pixel))
                using (SolidBrush capB = new SolidBrush(ColorTranslator.FromHtml("#848B87")))
                using (SolidBrush balB = new SolidBrush(ColorTranslator.FromHtml("#2B5A33")))
                using (SolidBrush subB = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                    g.DrawString("PERPETUAL ROLLING BALANCE", capF, capB, cx + 32f, cy + 26f);
                    g.DrawString("+$3,420.50", balF, balB, cx + 32f, cy + 62f);

                    using (Pen divPen = new Pen(ColorTranslator.FromHtml("#ECEBE5"), 1.5f)) {
                        g.DrawLine(divPen, cx + 32f, cy + 180f, cx + cw - 32f, cy + 180f);
                    }

                    // Status pill
                    g.DrawString("● Healthy Liquidity", capF, balB, cx + 32f, cy + 208f);
                    g.DrawString("▲ +$2,145.20 this cycle", subF, subB, cx + cw - 260f, cy + 208f);
                }

                // Stream Header
                float streamY = cy + ch + 32f;
                using (Font shF = new Font("Segoe UI", 20f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush shB = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                    g.DrawString("CONTINUOUS STREAM", shF, shB, cx + 4f, streamY);
                }

                // Transactions
                float itemY = streamY + 40f;
                float itemH = 112f;
                var items = new[] {
                    new { T = "Bi-Weekly Payday", C = "Automated Engine • Income", A = "+$2,450.00", Inc = true },
                    new { T = "Farmers Market & Pantry", C = "Groceries", A = "-$142.30", Inc = false },
                    new { T = "Clean Water & Electric", C = "Utilities", A = "-$84.50", Inc = false },
                    new { T = "Artisan Coffee Roasters", C = "Dining & Cafes", A = "-$6.80", Inc = false },
                    new { T = "Metro Monthly Transit Pass", C = "Transport", A = "-$78.00", Inc = false }
                };

                using (Font titF = new Font("Segoe UI", 24f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font catF = new Font("Segoe UI", 18f, FontStyle.Regular, GraphicsUnit.Pixel))
                using (Font amtF = new Font("Segoe UI", 26f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush titB = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
                using (SolidBrush catB = new SolidBrush(ColorTranslator.FromHtml("#848B87")))
                using (SolidBrush incB = new SolidBrush(ColorTranslator.FromHtml("#2B5A33")))
                using (SolidBrush expB = new SolidBrush(ColorTranslator.FromHtml("#8A3C30")))
                using (SolidBrush itemBg = new SolidBrush(Color.White))
                using (Pen itemBorder = new Pen(ColorTranslator.FromHtml("#EDEDE9"), 1.2f)) {
                    foreach (var it in items) {
                        using (GraphicsPath ip = RoundedRect(new RectangleF(cx, itemY, cw, itemH), 20f)) {
                            g.FillPath(itemBg, ip);
                            g.DrawPath(itemBorder, ip);
                        }

                        g.DrawString(it.T, titF, titB, cx + 24f, itemY + 22f);
                        g.DrawString(it.C, catF, catB, cx + 24f, itemY + 58f);

                        StringFormat rfmt = new StringFormat { Alignment = StringAlignment.Far };
                        g.DrawString(it.A, amtF, it.Inc ? incB : expB, new RectangleF(cx + cw - 260f, itemY + 36f, 236f, 40f), rfmt);

                        itemY += itemH + 16f;
                    }
                }

                DrawPhoneNavBar(g, sx, sy, sw, sh, 0, false);
                g.ResetClip();
            }

            bmp.Save(path, ImageFormat.Png);
        }
    }

    public static void GenerateScreenshot2(string path) {
        using (Bitmap bmp = new Bitmap(W, H, PixelFormat.Format24bppRgb))
        using (Graphics g = Graphics.FromImage(bmp)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            using (SolidBrush bg = new SolidBrush(ColorTranslator.FromHtml("#F3F2EE"))) {
                g.FillRectangle(bg, 0, 0, W, H);
            }

            using (Font tf = new Font("Segoe UI", 56f, FontStyle.Bold, GraphicsUnit.Pixel))
            using (Font sf = new Font("Segoe UI", 27f, FontStyle.Regular, GraphicsUnit.Pixel))
            using (SolidBrush tb = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
            using (SolidBrush sb = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                StringFormat fmt = new StringFormat { Alignment = StringAlignment.Center };
                g.DrawString("Tactile Transaction Ledger", tf, tb, new RectangleF(50, 110, W - 100, 80), fmt);
                g.DrawString("Log and review daily spending with effortless clarity", sf, sb, new RectangleF(50, 195, W - 100, 100), fmt);
            }

            float pw = 880f, ph = 1890f, px = (W - pw) / 2f, py = 430f;
            DrawPhoneFrame(g, px, py, pw, ph, false);

            float pad = 18f, sx = px + pad, sy = py + pad, sw = pw - pad * 2, sh = ph - pad * 2;
            using (GraphicsPath sp = RoundedRect(new RectangleF(sx, sy, sw, sh), 42f)) {
                g.SetClip(sp);
                using (SolidBrush sbg = new SolidBrush(ColorTranslator.FromHtml("#F9F9F8"))) g.FillRectangle(sbg, sx, sy, sw, sh);
                DrawPhoneStatusBar(g, sx, sy, sw, false);

                float topY = sy + 68f;
                using (Font hf = new Font("Segoe UI", 32f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush htb = new SolidBrush(ColorTranslator.FromHtml("#181C1B"))) {
                    g.DrawString("All Activity", hf, htb, sx + 36f, topY + 2f);
                }

                // Filter pills
                float chipY = topY + 60f;
                string[] chips = new string[] { "All", "Expenses", "Income", "Recurring" };
                float cx = sx + 36f;
                using (Font chipF = new Font("Segoe UI", 18f, FontStyle.Bold, GraphicsUnit.Pixel)) {
                    for (int i = 0; i < chips.Length; i++) {
                        bool active = (i == 0);
                        SizeF sz = g.MeasureString(chips[i], chipF);
                        float rw = sz.Width + 32f, rh = 46f;
                        using (GraphicsPath cp = RoundedRect(new RectangleF(cx, chipY, rw, rh), 23f)) {
                            using (SolidBrush cbg = new SolidBrush(active ? ColorTranslator.FromHtml("#181C1B") : Color.White))
                                g.FillPath(cbg, cp);
                            using (Pen cpen = new Pen(ColorTranslator.FromHtml(active ? "#181C1B" : "#E2E2DF"), 1.2f))
                                g.DrawPath(cpen, cp);
                            using (SolidBrush ctxt = new SolidBrush(active ? Color.White : ColorTranslator.FromHtml("#565D59"))) {
                                StringFormat sf = new StringFormat { Alignment = StringAlignment.Center, LineAlignment = StringAlignment.Center };
                                g.DrawString(chips[i], chipF, ctxt, new RectangleF(cx, chipY, rw, rh), sf);
                            }
                        }
                        cx += rw + 14f;
                    }
                }

                // Group 1: Today
                float listY = chipY + 68f;
                using (Font secF = new Font("Segoe UI", 19f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush secB = new SolidBrush(ColorTranslator.FromHtml("#848B87"))) {
                    g.DrawString("TODAY • OCTOBER 12", secF, secB, sx + 36f, listY);

                    var g1 = new[] {
                        new { T = "Specialty Coffee Roasters", C = "Dining & Cafes", A = "-$5.75", Inc = false },
                        new { T = "Whole Foods Organic Market", C = "Groceries", A = "-$86.40", Inc = false },
                        new { T = "Metro Transit Recharge", C = "Commute", A = "-$20.00", Inc = false }
                    };

                    listY += 34f;
                    using (Font titF = new Font("Segoe UI", 24f, FontStyle.Bold, GraphicsUnit.Pixel))
                    using (Font catF = new Font("Segoe UI", 18f, FontStyle.Regular, GraphicsUnit.Pixel))
                    using (Font amtF = new Font("Segoe UI", 26f, FontStyle.Bold, GraphicsUnit.Pixel))
                    using (SolidBrush titB = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
                    using (SolidBrush catB = new SolidBrush(ColorTranslator.FromHtml("#848B87")))
                    using (SolidBrush incB = new SolidBrush(ColorTranslator.FromHtml("#2B5A33")))
                    using (SolidBrush expB = new SolidBrush(ColorTranslator.FromHtml("#8A3C30")))
                    using (SolidBrush itemBg = new SolidBrush(Color.White))
                    using (Pen itemBorder = new Pen(ColorTranslator.FromHtml("#EDEDE9"), 1.2f)) {
                        float cw = sw - 72f;
                        foreach (var it in g1) {
                            using (GraphicsPath ip = RoundedRect(new RectangleF(sx + 36f, listY, cw, 108f), 20f)) {
                                g.FillPath(itemBg, ip);
                                g.DrawPath(itemBorder, ip);
                            }
                            g.DrawString(it.T, titF, titB, sx + 58f, listY + 22f);
                            g.DrawString(it.C, catF, catB, sx + 58f, listY + 56f);
                            StringFormat rfmt = new StringFormat { Alignment = StringAlignment.Far };
                            g.DrawString(it.A, amtF, it.Inc ? incB : expB, new RectangleF(sx + 36f + cw - 260f, listY + 34f, 236f, 40f), rfmt);
                            listY += 122f;
                        }

                        // Group 2: Yesterday
                        listY += 12f;
                        g.DrawString("YESTERDAY • OCTOBER 11", secF, secB, sx + 36f, listY);
                        listY += 34f;

                        var g2 = new[] {
                            new { T = "Client Milestone Payment", C = "Freelance • Income", A = "+$850.00", Inc = true },
                            new { T = "Gigabit Fiber Internet", C = "Utilities", A = "-$70.00", Inc = false },
                            new { T = "Independent Bookstore", C = "Education", A = "-$24.50", Inc = false },
                            new { T = "Neighborhood Pharmacy", C = "Health & Wellness", A = "-$18.25", Inc = false }
                        };

                        foreach (var it in g2) {
                            using (GraphicsPath ip = RoundedRect(new RectangleF(sx + 36f, listY, cw, 108f), 20f)) {
                                g.FillPath(itemBg, ip);
                                g.DrawPath(itemBorder, ip);
                            }
                            g.DrawString(it.T, titF, titB, sx + 58f, listY + 22f);
                            g.DrawString(it.C, catF, catB, sx + 58f, listY + 56f);
                            StringFormat rfmt = new StringFormat { Alignment = StringAlignment.Far };
                            g.DrawString(it.A, amtF, it.Inc ? incB : expB, new RectangleF(sx + 36f + cw - 260f, listY + 34f, 236f, 40f), rfmt);
                            listY += 122f;
                        }
                    }
                }

                // Floating Action Button (+)
                float fabSize = 74f;
                float fabX = sx + sw - fabSize - 36f;
                float fabY = sy + sh - 100f - fabSize - 26f;
                using (SolidBrush fabB = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
                using (Font fabFont = new Font("Segoe UI", 42f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush fabText = new SolidBrush(Color.White)) {
                    g.FillEllipse(fabB, fabX, fabY, fabSize, fabSize);
                    StringFormat cFmt = new StringFormat { Alignment = StringAlignment.Center, LineAlignment = StringAlignment.Center };
                    g.DrawString("+", fabFont, fabText, new RectangleF(fabX, fabY - 4f, fabSize, fabSize), cFmt);
                }

                DrawPhoneNavBar(g, sx, sy, sw, sh, 0, false);
                g.ResetClip();
            }

            bmp.Save(path, ImageFormat.Png);
        }
    }

    public static void GenerateScreenshot3(string path) {
        using (Bitmap bmp = new Bitmap(W, H, PixelFormat.Format24bppRgb))
        using (Graphics g = Graphics.FromImage(bmp)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            using (SolidBrush bg = new SolidBrush(ColorTranslator.FromHtml("#F3F2EE"))) {
                g.FillRectangle(bg, 0, 0, W, H);
            }

            using (Font tf = new Font("Segoe UI", 56f, FontStyle.Bold, GraphicsUnit.Pixel))
            using (Font sf = new Font("Segoe UI", 27f, FontStyle.Regular, GraphicsUnit.Pixel))
            using (SolidBrush tb = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
            using (SolidBrush sb = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                StringFormat fmt = new StringFormat { Alignment = StringAlignment.Center };
                g.DrawString("Automated Income Engine", tf, tb, new RectangleF(50, 110, W - 100, 80), fmt);
                g.DrawString("Set your pay cadence once — automatic credits on payday", sf, sb, new RectangleF(50, 195, W - 100, 100), fmt);
            }

            float pw = 880f, ph = 1890f, px = (W - pw) / 2f, py = 430f;
            DrawPhoneFrame(g, px, py, pw, ph, false);

            float pad = 18f, sx = px + pad, sy = py + pad, sw = pw - pad * 2, sh = ph - pad * 2;
            using (GraphicsPath sp = RoundedRect(new RectangleF(sx, sy, sw, sh), 42f)) {
                g.SetClip(sp);
                using (SolidBrush sbg = new SolidBrush(ColorTranslator.FromHtml("#F9F9F8"))) g.FillRectangle(sbg, sx, sy, sw, sh);
                DrawPhoneStatusBar(g, sx, sy, sw, false);

                // Dimmed background ledger
                using (SolidBrush scrim = new SolidBrush(Color.FromArgb(90, 0, 0, 0))) {
                    g.FillRectangle(scrim, sx, sy, sw, sh);
                }

                // Bottom Sheet Modal
                float sheetH = 1120f;
                float sheetY = sy + sh - sheetH;
                using (GraphicsPath sheetPath = RoundedRect(new RectangleF(sx, sheetY, sw, sheetH), 36f)) {
                    using (SolidBrush shBg = new SolidBrush(Color.White)) g.FillPath(shBg, sheetPath);
                }

                // Handle bar
                float hw = 64f, hh = 6f, hx = sx + (sw - hw) / 2f;
                using (SolidBrush hb = new SolidBrush(ColorTranslator.FromHtml("#D0CEC6")))
                using (GraphicsPath hp = RoundedRect(new RectangleF(hx, sheetY + 18f, hw, hh), 3f)) {
                    g.FillPath(hb, hp);
                }

                // Sheet Title & Description
                using (Font stF = new Font("Segoe UI", 34f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font sdF = new Font("Segoe UI", 21f, FontStyle.Regular, GraphicsUnit.Pixel))
                using (SolidBrush stB = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
                using (SolidBrush sdB = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                    g.DrawString("Income Engine Cadence", stF, stB, sx + 44f, sheetY + 54f);
                    g.DrawString("Automate recurring salary & client deposits directly into your rolling balance without manual entry.", sdF, sdB, new RectangleF(sx + 44f, sheetY + 104f, sw - 88f, 70f));
                }

                // Field 1: Amount
                float fy = sheetY + 190f;
                using (Font lblF = new Font("Segoe UI", 18f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font valF = new Font("Segoe UI", 36f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush lblB = new SolidBrush(ColorTranslator.FromHtml("#848B87")))
                using (SolidBrush valB = new SolidBrush(ColorTranslator.FromHtml("#2B5A33"))) {
                    g.DrawString("PAYCHECK AMOUNT", lblF, lblB, sx + 44f, fy);
                    fy += 30f;
                    using (GraphicsPath boxP = RoundedRect(new RectangleF(sx + 44f, fy, sw - 88f, 84f), 18f)) {
                        using (SolidBrush bbg = new SolidBrush(ColorTranslator.FromHtml("#F9F9F8"))) g.FillPath(bbg, boxP);
                        using (Pen bp = new Pen(ColorTranslator.FromHtml("#E2E2DF"), 1.5f)) g.DrawPath(bp, boxP);
                    }
                    g.DrawString("$2,450.00", valF, valB, sx + 68f, fy + 18f);
                }

                // Field 2: Cadence Options
                fy += 120f;
                using (Font lblF = new Font("Segoe UI", 18f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush lblB = new SolidBrush(ColorTranslator.FromHtml("#848B87"))) {
                    g.DrawString("RECURRENCE CADENCE", lblF, lblB, sx + 44f, fy);
                }

                fy += 32f;
                string[] cadences = new string[] { "Weekly", "Bi-Weekly", "Semi-Mo", "Monthly" };
                float cW = (sw - 88f - 36f) / 4f;
                using (Font cadF = new Font("Segoe UI", 18f, FontStyle.Bold, GraphicsUnit.Pixel)) {
                    for (int i = 0; i < 4; i++) {
                        bool active = (i == 1);
                        float ix = sx + 44f + (i * (cW + 12f));
                        using (GraphicsPath cp = RoundedRect(new RectangleF(ix, fy, cW, 64f), 16f)) {
                            using (SolidBrush cbg = new SolidBrush(ColorTranslator.FromHtml(active ? "#2B5A33" : "#F4F3EE")))
                                g.FillPath(cbg, cp);
                            using (SolidBrush ctxt = new SolidBrush(active ? Color.White : ColorTranslator.FromHtml("#565D59"))) {
                                StringFormat sf = new StringFormat { Alignment = StringAlignment.Center, LineAlignment = StringAlignment.Center };
                                g.DrawString(cadences[i], cadF, ctxt, new RectangleF(ix, fy, cW, 64f), sf);
                            }
                        }
                    }
                }

                // Field 3: Next Deposit Date
                fy += 96f;
                using (Font lblF = new Font("Segoe UI", 18f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font valF = new Font("Segoe UI", 24f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush lblB = new SolidBrush(ColorTranslator.FromHtml("#848B87")))
                using (SolidBrush valB = new SolidBrush(ColorTranslator.FromHtml("#181C1B"))) {
                    g.DrawString("NEXT DEPOSIT DATE", lblF, lblB, sx + 44f, fy);
                    fy += 30f;
                    using (GraphicsPath boxP = RoundedRect(new RectangleF(sx + 44f, fy, sw - 88f, 76f), 18f)) {
                        using (SolidBrush bbg = new SolidBrush(ColorTranslator.FromHtml("#F9F9F8"))) g.FillPath(bbg, boxP);
                        using (Pen bp = new Pen(ColorTranslator.FromHtml("#E2E2DF"), 1.5f)) g.DrawPath(bp, boxP);
                    }
                    g.DrawString("📅   Friday, October 24, 2026", valF, valB, sx + 68f, fy + 22f);
                }

                // Toggle Card
                fy += 108f;
                using (GraphicsPath togP = RoundedRect(new RectangleF(sx + 44f, fy, sw - 88f, 96f), 20f)) {
                    using (SolidBrush tbg = new SolidBrush(ColorTranslator.FromHtml("#EDF4EE"))) g.FillPath(tbg, togP);
                    using (Pen tp = new Pen(ColorTranslator.FromHtml("#BCD5C1"), 1.5f)) g.DrawPath(tp, togP);
                }
                using (Font ttF = new Font("Segoe UI", 21f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font tsF = new Font("Segoe UI", 17f, FontStyle.Regular, GraphicsUnit.Pixel))
                using (SolidBrush ttB = new SolidBrush(ColorTranslator.FromHtml("#2B5A33")))
                using (SolidBrush tsB = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                    g.DrawString("Automatic Ledger Credit", ttF, ttB, sx + 68f, fy + 22f);
                    g.DrawString("Credit rolling balance automatically on payday", tsF, tsB, sx + 68f, fy + 52f);
                    // Switch
                    float swX = sx + sw - 140f, swY = fy + 28f;
                    using (SolidBrush sActive = new SolidBrush(ColorTranslator.FromHtml("#2B5A33")))
                    using (GraphicsPath spSwitch = RoundedRect(new RectangleF(swX, swY, 68f, 38f), 19f)) {
                        g.FillPath(sActive, spSwitch);
                        using (SolidBrush wCircle = new SolidBrush(Color.White)) {
                            g.FillEllipse(wCircle, swX + 34f, swY + 4f, 30f, 30f);
                        }
                    }
                }

                // CTA Button
                fy += 128f;
                using (GraphicsPath btnP = RoundedRect(new RectangleF(sx + 44f, fy, sw - 88f, 84f), 22f)) {
                    using (SolidBrush bbg = new SolidBrush(ColorTranslator.FromHtml("#181C1B"))) g.FillPath(bbg, btnP);
                    using (Font btnF = new Font("Segoe UI", 24f, FontStyle.Bold, GraphicsUnit.Pixel))
                    using (SolidBrush btnTxt = new SolidBrush(Color.White)) {
                        StringFormat sf = new StringFormat { Alignment = StringAlignment.Center, LineAlignment = StringAlignment.Center };
                        g.DrawString("Save Income Cadence", btnF, btnTxt, new RectangleF(sx + 44f, fy, sw - 88f, 84f), sf);
                    }
                }

                DrawPhoneNavBar(g, sx, sy, sw, sh, 0, false);
                g.ResetClip();
            }

            bmp.Save(path, ImageFormat.Png);
        }
    }

    public static void GenerateScreenshot4(string path) {
        using (Bitmap bmp = new Bitmap(W, H, PixelFormat.Format24bppRgb))
        using (Graphics g = Graphics.FromImage(bmp)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            using (SolidBrush bg = new SolidBrush(ColorTranslator.FromHtml("#F3F2EE"))) {
                g.FillRectangle(bg, 0, 0, W, H);
            }

            using (Font tf = new Font("Segoe UI", 56f, FontStyle.Bold, GraphicsUnit.Pixel))
            using (Font sf = new Font("Segoe UI", 27f, FontStyle.Regular, GraphicsUnit.Pixel))
            using (SolidBrush tb = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
            using (SolidBrush sb = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                StringFormat fmt = new StringFormat { Alignment = StringAlignment.Center };
                g.DrawString("Tranquil Flow Analytics", tf, tb, new RectangleF(50, 110, W - 100, 80), fmt);
                g.DrawString("Understand cash flow & spending trends without the anxiety", sf, sb, new RectangleF(50, 195, W - 100, 100), fmt);
            }

            float pw = 880f, ph = 1890f, px = (W - pw) / 2f, py = 430f;
            DrawPhoneFrame(g, px, py, pw, ph, false);

            float pad = 18f, sx = px + pad, sy = py + pad, sw = pw - pad * 2, sh = ph - pad * 2;
            using (GraphicsPath sp = RoundedRect(new RectangleF(sx, sy, sw, sh), 42f)) {
                g.SetClip(sp);
                using (SolidBrush sbg = new SolidBrush(ColorTranslator.FromHtml("#F9F9F8"))) g.FillRectangle(sbg, sx, sy, sw, sh);
                DrawPhoneStatusBar(g, sx, sy, sw, false);

                float topY = sy + 68f;
                using (Font hf = new Font("Segoe UI", 32f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush htb = new SolidBrush(ColorTranslator.FromHtml("#181C1B"))) {
                    g.DrawString("Flow Analytics", hf, htb, sx + 36f, topY + 2f);
                }

                // Summary Net Card
                float cx = sx + 36f, cy = topY + 64f, cw = sw - 72f;
                using (GraphicsPath cp = RoundedRect(new RectangleF(cx, cy, cw, 250f), 26f)) {
                    using (SolidBrush cbg = new SolidBrush(Color.White)) g.FillPath(cbg, cp);
                    using (Pen cpPen = new Pen(ColorTranslator.FromHtml("#E2E2DF"), 1.5f)) g.DrawPath(cpPen, cp);
                }

                using (Font capF = new Font("Segoe UI", 18f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font numF = new Font("Segoe UI", 36f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush capB = new SolidBrush(ColorTranslator.FromHtml("#848B87")))
                using (SolidBrush incB = new SolidBrush(ColorTranslator.FromHtml("#2B5A33")))
                using (SolidBrush expB = new SolidBrush(ColorTranslator.FromHtml("#8A3C30"))) {
                    g.DrawString("INCOME THIS CYCLE", capF, capB, cx + 28f, cy + 24f);
                    g.DrawString("+$4,900.00", numF, incB, cx + 28f, cy + 54f);

                    g.DrawString("TOTAL EXPENSES", capF, capB, cx + cw / 2f + 10f, cy + 24f);
                    g.DrawString("-$2,154.50", numF, expB, cx + cw / 2f + 10f, cy + 54f);

                    // Flow comparison bar
                    float barY = cy + 130f;
                    float barW = cw - 56f;
                    float incBarW = barW * 0.69f;
                    using (GraphicsPath ib = RoundedRect(new RectangleF(cx + 28f, barY, incBarW, 18f), 9f)) {
                        using (SolidBrush b = new SolidBrush(ColorTranslator.FromHtml("#3A5A40"))) g.FillPath(b, ib);
                    }
                    using (GraphicsPath eb = RoundedRect(new RectangleF(cx + 28f + incBarW + 6f, barY, barW - incBarW - 6f, 18f), 9f)) {
                        using (SolidBrush b = new SolidBrush(ColorTranslator.FromHtml("#8A3C30"))) g.FillPath(b, eb);
                    }

                    using (Font netF = new Font("Segoe UI", 21f, FontStyle.Bold, GraphicsUnit.Pixel))
                    using (SolidBrush netB = new SolidBrush(ColorTranslator.FromHtml("#2B5A33"))) {
                        g.DrawString("Net Savings: +$2,745.50 (56% Savings Rate)", netF, netB, cx + 28f, cy + 185f);
                    }
                }

                // Section: Breakdown by Category
                float secY = cy + 280f;
                using (Font secF = new Font("Segoe UI", 20f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush secB = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                    g.DrawString("SPENDING BY CATEGORY", secF, secB, cx, secY);
                }

                var cats = new[] {
                    new { N = "Housing & Rent", A = "$1,200.00", P = "55.7%", Col = "#3A5A40", Bar = 0.557f },
                    new { N = "Groceries & Kitchen", A = "$480.20", P = "22.3%", Col = "#588157", Bar = 0.223f },
                    new { N = "Utilities & Bills", A = "$245.80", P = "11.4%", Col = "#8A3C30", Bar = 0.114f },
                    new { N = "Dining & Cafes", A = "$148.50", P = "6.9%", Col = "#DDA15E", Bar = 0.069f },
                    new { N = "Health & Wellness", A = "$80.00", P = "3.7%", Col = "#6B705C", Bar = 0.037f }
                };

                float rowY = secY + 40f;
                using (Font namF = new Font("Segoe UI", 22f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font valF = new Font("Segoe UI", 21f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font pctF = new Font("Segoe UI", 18f, FontStyle.Regular, GraphicsUnit.Pixel))
                using (SolidBrush namB = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
                using (SolidBrush pctB = new SolidBrush(ColorTranslator.FromHtml("#848B87"))) {
                    foreach (var c in cats) {
                        using (GraphicsPath cp = RoundedRect(new RectangleF(cx, rowY, cw, 114f), 20f)) {
                            using (SolidBrush cb = new SolidBrush(Color.White)) g.FillPath(cb, cp);
                            using (Pen cpen = new Pen(ColorTranslator.FromHtml("#EDEDE9"), 1.2f)) g.DrawPath(cpen, cp);
                        }

                        g.DrawString(c.N, namF, namB, cx + 24f, rowY + 20f);
                        StringFormat rf = new StringFormat { Alignment = StringAlignment.Far };
                        g.DrawString(c.A + " (" + c.P + ")", valF, namB, new RectangleF(cx + cw - 280f, rowY + 20f, 256f, 30f), rf);

                        // Horizontal distribution bar
                        float pbW = cw - 48f;
                        using (SolidBrush barBg = new SolidBrush(ColorTranslator.FromHtml("#F2F1ED")))
                        using (GraphicsPath bbp = RoundedRect(new RectangleF(cx + 24f, rowY + 68f, pbW, 14f), 7f)) {
                            g.FillPath(barBg, bbp);
                        }
                        using (SolidBrush fillBg = new SolidBrush(ColorTranslator.FromHtml(c.Col)))
                        using (GraphicsPath fbp = RoundedRect(new RectangleF(cx + 24f, rowY + 68f, pbW * c.Bar, 14f), 7f)) {
                            g.FillPath(fillBg, fbp);
                        }

                        rowY += 130f;
                    }
                }

                DrawPhoneNavBar(g, sx, sy, sw, sh, 1, false);
                g.ResetClip();
            }

            bmp.Save(path, ImageFormat.Png);
        }
    }

    public static void GenerateScreenshot5(string path) {
        using (Bitmap bmp = new Bitmap(W, H, PixelFormat.Format24bppRgb))
        using (Graphics g = Graphics.FromImage(bmp)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            using (SolidBrush bg = new SolidBrush(ColorTranslator.FromHtml("#F3F2EE"))) {
                g.FillRectangle(bg, 0, 0, W, H);
            }

            using (Font tf = new Font("Segoe UI", 56f, FontStyle.Bold, GraphicsUnit.Pixel))
            using (Font sf = new Font("Segoe UI", 27f, FontStyle.Regular, GraphicsUnit.Pixel))
            using (SolidBrush tb = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
            using (SolidBrush sb = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                StringFormat fmt = new StringFormat { Alignment = StringAlignment.Center };
                g.DrawString("100% Local & Private", tf, tb, new RectangleF(50, 110, W - 100, 80), fmt);
                g.DrawString("Zero cloud sync, zero trackers, zero accounts. Stored solely on device", sf, sb, new RectangleF(50, 195, W - 100, 100), fmt);
            }

            float pw = 880f, ph = 1890f, px = (W - pw) / 2f, py = 430f;
            DrawPhoneFrame(g, px, py, pw, ph, false);

            float pad = 18f, sx = px + pad, sy = py + pad, sw = pw - pad * 2, sh = ph - pad * 2;
            using (GraphicsPath sp = RoundedRect(new RectangleF(sx, sy, sw, sh), 42f)) {
                g.SetClip(sp);
                using (SolidBrush sbg = new SolidBrush(ColorTranslator.FromHtml("#F9F9F8"))) g.FillRectangle(sbg, sx, sy, sw, sh);
                DrawPhoneStatusBar(g, sx, sy, sw, false);

                float topY = sy + 68f;
                using (Font hf = new Font("Segoe UI", 32f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush htb = new SolidBrush(ColorTranslator.FromHtml("#181C1B"))) {
                    g.DrawString("Settings & Privacy", hf, htb, sx + 36f, topY + 2f);
                }

                // Privacy Hero Shield Card
                float cx = sx + 36f, cy = topY + 64f, cw = sw - 72f;
                using (GraphicsPath cp = RoundedRect(new RectangleF(cx, cy, cw, 340f), 26f)) {
                    using (SolidBrush cbg = new SolidBrush(ColorTranslator.FromHtml("#EDF4EE"))) g.FillPath(cbg, cp);
                    using (Pen cpPen = new Pen(ColorTranslator.FromHtml("#BCD5C1"), 1.5f)) g.DrawPath(cpPen, cp);
                }

                using (Font shieldF = new Font("Segoe UI", 48f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font titleF = new Font("Segoe UI", 26f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font bodyF = new Font("Segoe UI", 20f, FontStyle.Regular, GraphicsUnit.Pixel))
                using (SolidBrush sText = new SolidBrush(ColorTranslator.FromHtml("#2B5A33")))
                using (SolidBrush bText = new SolidBrush(ColorTranslator.FromHtml("#181C1B"))) {
                    g.DrawString("🛡️", shieldF, sText, cx + 28f, cy + 24f);
                    g.DrawString("Zero-Knowledge Local Storage", titleF, sText, cx + 100f, cy + 34f);

                    string[] checks = new string[] {
                        "✔  100% on-device SQLite database (Android Room)",
                        "✔  Zero cloud sync, servers, or external telemetry",
                        "✔  Zero third-party tracking or advertising SDKs",
                        "✔  Full offline capability with Airplane mode enabled"
                    };

                    float chkY = cy + 110f;
                    foreach (string chk in checks) {
                        g.DrawString(chk, bodyF, bText, cx + 32f, chkY);
                        chkY += 48f;
                    }
                }

                // Data Controls Section
                float secY = cy + 370f;
                using (Font secF = new Font("Segoe UI", 20f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush secB = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                    g.DrawString("DATA OWNERSHIP & EXPORT", secF, secB, cx, secY);
                }

                var options = new[] {
                    new { T = "Export Encrypted Backup", S = "JSON archive of your entire ledger & settings" },
                    new { T = "Import Database Backup", S = "Restore previously exported ledger archive" },
                    new { T = "Wipe All Local Records", S = "Permanently purge SQLite database on this device" }
                };

                float optY = secY + 40f;
                using (Font opF = new Font("Segoe UI", 22f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font opsF = new Font("Segoe UI", 18f, FontStyle.Regular, GraphicsUnit.Pixel))
                using (SolidBrush opB = new SolidBrush(ColorTranslator.FromHtml("#181C1B")))
                using (SolidBrush opsB = new SolidBrush(ColorTranslator.FromHtml("#848B87"))) {
                    foreach (var op in options) {
                        using (GraphicsPath opp = RoundedRect(new RectangleF(cx, optY, cw, 110f), 20f)) {
                            using (SolidBrush obg = new SolidBrush(Color.White)) g.FillPath(obg, opp);
                            using (Pen open = new Pen(ColorTranslator.FromHtml("#EDEDE9"), 1.2f)) g.DrawPath(open, opp);
                        }
                        g.DrawString(op.T, opF, opB, cx + 24f, optY + 22f);
                        g.DrawString(op.S, opsF, opsB, cx + 24f, optY + 58f);
                        optY += 128f;
                    }
                }

                // Open Source Badge
                using (GraphicsPath osp = RoundedRect(new RectangleF(cx, optY + 10f, cw, 80f), 20f)) {
                    using (SolidBrush obg = new SolidBrush(ColorTranslator.FromHtml("#F2F1ED"))) g.FillPath(obg, osp);
                }
                using (Font osF = new Font("Segoe UI", 20f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush osB = new SolidBrush(ColorTranslator.FromHtml("#565D59"))) {
                    StringFormat sf = new StringFormat { Alignment = StringAlignment.Center, LineAlignment = StringAlignment.Center };
                    g.DrawString("100% Free & Open-Source Android App", osF, osB, new RectangleF(cx, optY + 10f, cw, 80f), sf);
                }

                DrawPhoneNavBar(g, sx, sy, sw, sh, 2, false);
                g.ResetClip();
            }

            bmp.Save(path, ImageFormat.Png);
        }
    }

    public static void GenerateScreenshot6(string path) {
        using (Bitmap bmp = new Bitmap(W, H, PixelFormat.Format24bppRgb))
        using (Graphics g = Graphics.FromImage(bmp)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            // Dark Mode Canvas
            using (SolidBrush bg = new SolidBrush(ColorTranslator.FromHtml("#161A18"))) {
                g.FillRectangle(bg, 0, 0, W, H);
            }

            using (Font tf = new Font("Segoe UI", 56f, FontStyle.Bold, GraphicsUnit.Pixel))
            using (Font sf = new Font("Segoe UI", 27f, FontStyle.Regular, GraphicsUnit.Pixel))
            using (SolidBrush tb = new SolidBrush(ColorTranslator.FromHtml("#F4F5F4")))
            using (SolidBrush sb = new SolidBrush(ColorTranslator.FromHtml("#A3ACA7"))) {
                StringFormat fmt = new StringFormat { Alignment = StringAlignment.Center };
                g.DrawString("Calm by Design", tf, tb, new RectangleF(50, 110, W - 100, 80), fmt);
                g.DrawString("Tactile organic tones with seamless Light & Dark themes", sf, sb, new RectangleF(50, 195, W - 100, 100), fmt);
            }

            float pw = 880f, ph = 1890f, px = (W - pw) / 2f, py = 430f;
            DrawPhoneFrame(g, px, py, pw, ph, true);

            float pad = 18f, sx = px + pad, sy = py + pad, sw = pw - pad * 2, sh = ph - pad * 2;
            using (GraphicsPath sp = RoundedRect(new RectangleF(sx, sy, sw, sh), 42f)) {
                g.SetClip(sp);
                using (SolidBrush sbg = new SolidBrush(ColorTranslator.FromHtml("#121514"))) g.FillRectangle(sbg, sx, sy, sw, sh);
                DrawPhoneStatusBar(g, sx, sy, sw, true);

                float topY = sy + 68f;
                using (SolidBrush lb = new SolidBrush(ColorTranslator.FromHtml("#588157"))) {
                    g.FillEllipse(lb, sx + 36f, topY, 44f, 44f);
                }
                using (SolidBrush db = new SolidBrush(ColorTranslator.FromHtml("#A3B18A"))) {
                    g.FillEllipse(db, sx + 50f, topY + 9f, 16f, 16f);
                }
                using (Font hf = new Font("Segoe UI", 32f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font mf = new Font("Segoe UI", 22f, FontStyle.Regular, GraphicsUnit.Pixel))
                using (SolidBrush htb = new SolidBrush(ColorTranslator.FromHtml("#F4F5F4")))
                using (SolidBrush mtb = new SolidBrush(ColorTranslator.FromHtml("#6D7671"))) {
                    g.DrawString("Base Ledger", hf, htb, sx + 96f, topY + 2f);
                    g.DrawString("Dark Olive Mode", mf, mtb, sx + sw - 210f, topY + 8f);
                }

                // Dark Rolling Balance Card
                float cx = sx + 32f, cy = topY + 74f, cw = sw - 64f, ch = 280f;
                using (GraphicsPath cp = RoundedRect(new RectangleF(cx, cy, cw, ch), 28f)) {
                    using (SolidBrush cbg = new SolidBrush(ColorTranslator.FromHtml("#1E2321"))) g.FillPath(cbg, cp);
                    using (Pen cpPen = new Pen(ColorTranslator.FromHtml("#2F3633"), 1.5f)) g.DrawPath(cpPen, cp);
                }

                using (Font capF = new Font("Segoe UI", 18f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font balF = new Font("Segoe UI", 68f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font subF = new Font("Segoe UI", 21f, FontStyle.Regular, GraphicsUnit.Pixel))
                using (SolidBrush capB = new SolidBrush(ColorTranslator.FromHtml("#6D7671")))
                using (SolidBrush balB = new SolidBrush(ColorTranslator.FromHtml("#6AA576")))
                using (SolidBrush subB = new SolidBrush(ColorTranslator.FromHtml("#A3ACA7"))) {
                    g.DrawString("PERPETUAL ROLLING BALANCE", capF, capB, cx + 32f, cy + 26f);
                    g.DrawString("+$3,420.50", balF, balB, cx + 32f, cy + 62f);

                    using (Pen divPen = new Pen(ColorTranslator.FromHtml("#2A322F"), 1.5f)) {
                        g.DrawLine(divPen, cx + 32f, cy + 180f, cx + cw - 32f, cy + 180f);
                    }

                    g.DrawString("● Healthy Liquidity", capF, balB, cx + 32f, cy + 208f);
                    g.DrawString("▲ +$2,145.20 this cycle", subF, subB, cx + cw - 260f, cy + 208f);
                }

                // Dark stream
                float streamY = cy + ch + 32f;
                using (Font shF = new Font("Segoe UI", 20f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush shB = new SolidBrush(ColorTranslator.FromHtml("#A3ACA7"))) {
                    g.DrawString("CONTINUOUS STREAM", shF, shB, cx + 4f, streamY);
                }

                float itemY = streamY + 40f;
                float itemH = 112f;
                var items = new[] {
                    new { T = "Bi-Weekly Payday", C = "Automated Engine • Income", A = "+$2,450.00", Inc = true },
                    new { T = "Farmers Market & Pantry", C = "Groceries", A = "-$142.30", Inc = false },
                    new { T = "Clean Water & Electric", C = "Utilities", A = "-$84.50", Inc = false },
                    new { T = "Artisan Coffee Roasters", C = "Dining & Cafes", A = "-$6.80", Inc = false },
                    new { T = "Metro Monthly Transit Pass", C = "Transport", A = "-$78.00", Inc = false }
                };

                using (Font titF = new Font("Segoe UI", 24f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font catF = new Font("Segoe UI", 18f, FontStyle.Regular, GraphicsUnit.Pixel))
                using (Font amtF = new Font("Segoe UI", 26f, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush titB = new SolidBrush(ColorTranslator.FromHtml("#F4F5F4")))
                using (SolidBrush catB = new SolidBrush(ColorTranslator.FromHtml("#6D7671")))
                using (SolidBrush incB = new SolidBrush(ColorTranslator.FromHtml("#6AA576")))
                using (SolidBrush expB = new SolidBrush(ColorTranslator.FromHtml("#E27D6F")))
                using (SolidBrush itemBg = new SolidBrush(ColorTranslator.FromHtml("#1E2321")))
                using (Pen itemBorder = new Pen(ColorTranslator.FromHtml("#2B3330"), 1.2f)) {
                    foreach (var it in items) {
                        using (GraphicsPath ip = RoundedRect(new RectangleF(cx, itemY, cw, itemH), 20f)) {
                            g.FillPath(itemBg, ip);
                            g.DrawPath(itemBorder, ip);
                        }
                        g.DrawString(it.T, titF, titB, cx + 24f, itemY + 22f);
                        g.DrawString(it.C, catF, catB, cx + 24f, itemY + 58f);

                        StringFormat rfmt = new StringFormat { Alignment = StringAlignment.Far };
                        g.DrawString(it.A, amtF, it.Inc ? incB : expB, new RectangleF(cx + cw - 260f, itemY + 36f, 236f, 40f), rfmt);
                        itemY += itemH + 16f;
                    }
                }

                DrawPhoneNavBar(g, sx, sy, sw, sh, 0, true);
                g.ResetClip();
            }

            bmp.Save(path, ImageFormat.Png);
        }
    }
}
"@

Add-Type -TypeDefinition $csharpCode -ReferencedAssemblies System.Drawing

$outDir = "d:\Workspace\base\play_store_assets"
[PlayStoreScreenshotBuilder]::GenerateScreenshot1("$outDir\screenshot_1_rolling_balance.png")
[PlayStoreScreenshotBuilder]::GenerateScreenshot2("$outDir\screenshot_2_ledger_stream.png")
[PlayStoreScreenshotBuilder]::GenerateScreenshot3("$outDir\screenshot_3_income_engine.png")
[PlayStoreScreenshotBuilder]::GenerateScreenshot4("$outDir\screenshot_4_flow_analytics.png")
[PlayStoreScreenshotBuilder]::GenerateScreenshot5("$outDir\screenshot_5_local_privacy.png")
[PlayStoreScreenshotBuilder]::GenerateScreenshot6("$outDir\screenshot_6_dark_mode.png")

Write-Output "All 6 Play Store screenshots successfully rendered!"
