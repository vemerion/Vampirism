package de.teamlapen.lib.lib.client.gui;


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import de.teamlapen.lib.LIBREFERENCE;
import de.teamlapen.lib.lib.util.UtilLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;


/**
 * Radial/pie menu which can be used as screen overlay to select stuff The angles (in radiant) used in this class are used to describe the direction from the screen center. 0/2Pi shows right, Pi/2 up
 * (!negative Y), Pi left ... (similar to the visualization of complex numbers.
 *
 * @author maxanier
 */
@OnlyIn(Dist.CLIENT)
public abstract class GuiPieMenu<T> extends Screen {
    private final static ResourceLocation backgroundTex = new ResourceLocation(LIBREFERENCE.MODID, "textures/gui/pie_menu_bg.png");
    private final static ResourceLocation centerTex = new ResourceLocation(LIBREFERENCE.MODID, "textures/gui/pie_menu_center.png");
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    protected final ArrayList<T> elements;

    protected final Color backgroundColor;
    /**
     * transparency of the background
     */
    private final float BGT = 0.7f;
    /**
     * Size of the background image
     */
    private final int BGS = 300;
    /**
     * Radius of the ring in the middle
     */
    private final int RR = 60;
    /**
     * Size of the images for the center
     */
    private final int CS = 100;

    private int selectedElement = -1;
    private int elementCount;
    /**
     * Angle between each element in rad
     */
    private double radDiff;

    public GuiPieMenu(Color backgroundColorIn, ITextComponent title) {
        super(title);
        this.field_230711_n_ = true; //passEvents
        this.backgroundColor = backgroundColorIn;
        this.elements = new ArrayList<>();
    }

    @Override
    public void func_230430_a_(MatrixStack stack, int mouseX, int mouseY, float partialTicks) { //render
        // Calculate center and radius of the skill cycle
        int cX = this.field_230708_k_ / 2;
        int cY = this.field_230709_l_ / 2;
        double radius = this.field_230709_l_ / 4d;

        drawBackground(cX, cY);
        // Check if the mouse is in bounds and whether its in the center or not
        double mouseRad = updateMouse(mouseX, mouseY, cX, cY, radius / 2);
        boolean center = (mouseX - cX) * (mouseX - cX) + (mouseY - cY) * (mouseY - cY) < (radius / 4) * (radius / 4);
        if (center)
            selectedElement = -1;
        // Draw each skill
        for (int i = 0; i < elementCount; i++) {
            T element = elements.get(i);

            // Check if the mouse cursor is in the area of this element
            double rad = radDiff * i;
            boolean selected = false;
            if (!center && mouseRad > rad - radDiff / 2D && mouseRad < (rad + (radDiff / 2D))) {
                selected = true;
            } else if (!center && rad == 0 && mouseRad > 2D * Math.PI - radDiff / 2D) {
                selected = true;
            }
            int x = (int) (cX + Math.cos(rad) * radius) - 16 / 2;
            int y = (int) (cY - Math.sin(rad) * radius) - 16 / 2;

            // Draw box and, if selected, highlight
            Color col = this.getColor(element);
            RenderSystem.color4f(col.getRed(), col.getGreen(), col.getBlue(), 0.5F);
            this.field_230706_i_
                    .getTextureManager().bindTexture(WIDGETS);
            func_238474_b_
                    (stack, x - 2, y - 2, 1, 1, 20, 20);
            if (selected) {
                func_238474_b_
                        (stack, x - 3, y - 3, 1, 23, 22, 22);
            }
            if (selected) {
                selectedElement = i;
                drawSelectedCenter(cX, cY, rad);
            }
            // Draw Icon
            RenderSystem.color4f(1F, 1F, 1F, 1F);
            this.field_230706_i_
                    .getTextureManager().bindTexture(getIconLoc(element));
            UtilLib.drawTexturedModalRect(func_230927_p_
                    (), x, y, 0, 0, 16, 16, 16, 16);

            this.afterIconDraw(element, x, y);

        }
        if (selectedElement == -1) {
            this.drawUnselectedCenter(cX, cY);
        } else {
            String name = UtilLib.translate(getUnlocalizedName(elements.get(selectedElement)));
            int tx = cX - field_230706_i_
                    .fontRenderer.getStringWidth(name) / 2;
            int ty = this.field_230709_l_
                    / 7;
            field_230706_i_
                    .fontRenderer.func_238405_a_
                    (stack, name, tx, ty, Color.WHITE.getRGB());
        }
        super.func_230430_a_(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void func_231023_e_() { //tick
        super.func_231023_e_();
        if (this.field_230706_i_
                != null && this.field_230706_i_
                .player != null) {
            if (!this.field_230706_i_
                    .player.isAlive()) {
                this.field_230706_i_
                        .player.closeScreen();
            } else {
                this.field_230706_i_
                        .player.movementInput.func_225607_a_(this.field_230706_i_
                        .player.func_228354_I_()); //tick, shouldRenderSneaking
            }
        }
    }

    @Override
    public boolean keyReleased(int key, int scancode, int modifiers) {
        if (!isKeyBindingStillPressed()) {
            this.selectedAndClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean func_231043_a_(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double p_mouseScrolled_5_) {
        return false;
    }

    @Override
    public boolean func_231048_c_(double p_mouseReleased_1_, double p_mouseReleased_3_, int p_mouseReleased_5_) { //mouseReleased
        if (!isKeyBindingStillPressed()) {
            this.selectedAndClose();
            return true;
        }
        return false;
    }

    @Override
    public void func_231160_c_() {
        this.onGuiInit();
        this.elementCount = elements.size();
        radDiff = 2D * Math.PI / elementCount;// gap in rad
        GLFW.glfwSetInputMode(field_230706_i_.getMainWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        ForgeIngameGui.renderCrosshairs = false;
    }

    @Override
    public void func_231164_f_() { //Removed
        super.func_231164_f_();
        GLFW.glfwSetInputMode(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        ForgeIngameGui.renderCrosshairs = true;
    }

    @Override
    public void func_231175_as__() {
        super.func_231175_as__();
        ForgeIngameGui.renderCrosshairs = true;
    }

    @Override
    public boolean func_231177_au__() {
        return false;
    }

    protected boolean isKeyBindingStillPressed() {
        return getMenuKeyBinding().isKeyDown();
    }

    protected void afterIconDraw(T element, int x, int y) {

    }

    /**
     * Draws a line between the given coordinates
     */
    protected void drawLine(double x1, double y1, double x2, double y2) {
        RenderSystem.disableTexture();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        builder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        RenderSystem.lineWidth(2F);

        builder.pos(x1, y1, this.func_230927_p_
                ()).endVertex();
        builder.pos(x2, y2, this.func_230927_p_
                ()).endVertex();
        Tessellator.getInstance().draw();
        RenderSystem.enableTexture();
    }

    /**
     * This method is called to retrieve the color for the elements border
     *
     * @param s
     * @return Color
     */
    @Nonnull
    protected Color getColor(T s) {
        return Color.WHITE;
    }

    /**
     * @param item
     * @return the location of the icon map where the icon for the given item is in
     */
    protected abstract ResourceLocation getIconLoc(T item);

    /**
     * @return the menu key binding set in the game settings
     */
    protected abstract KeyBinding getMenuKeyBinding();

    protected void selectedAndClose() {
        func_231175_as__
                ();
        if (selectedElement >= 0) {
            this.onElementSelected(elements.get(selectedElement));
        }
    }

    protected int getSelectedElement() {
        return selectedElement;
    }

    protected abstract String getUnlocalizedName(T item);

    protected void onElementSelected(T id) {

    }

    protected abstract void onGuiInit();

    /**
     * Draws the background cicle image as well as border lines between the different segments
     *
     * @param cX CenterX
     * @param cY CenterY
     */
    private void drawBackground(float cX, float cY) {
        // Calculate the scale which has to be applied for the image to fit
        float scale = (this.field_230709_l_
                / 2F + 16 + 16) / BGS;

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.translatef(cX, cY, this.func_230927_p_
                ());
        RenderSystem.scalef(scale, scale, 1);

        // Draw the cicle image
        this.field_230706_i_
                .getTextureManager().bindTexture(backgroundTex);
        RenderSystem.color4f(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue(), BGT);


        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        builder.pos(BGS / 2f, BGS / 2f, this.func_230927_p_
                ()).tex(1, 1).endVertex();
        builder.pos(BGS / 2f, -BGS / 2f, this.func_230927_p_
                ()).tex(1, 0).endVertex();
        builder.pos(-BGS / 2f, -BGS / 2f, this.func_230927_p_
                ()).tex(0, 0).endVertex();
        builder.pos(-BGS / 2f, BGS / 2f, this.func_230927_p_
                ()).tex(0, 1).endVertex();
        tessellator.draw();


        // Draw the lines
        if (elementCount > 1) {
            for (int i = 0; i < elementCount; i++) {
                double rad = i * radDiff + radDiff / 2;
                double cos = Math.cos(rad);
                double sin = Math.sin(rad);
                this.drawLine(cos * RR, sin * RR, +cos * BGS / 2, sin * BGS / 2);
            }
        }
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();

    }

    /**
     * Draws a circle with an arrow at the given coords
     *
     * @param cX
     * @param cY
     * @param rad The direction the arrow should point in radiant
     */
    private void drawSelectedCenter(double cX, double cY, double rad) {

        // Caluculate rotation and scale
        double deg = Math.toDegrees(-rad);
        float scale = (this.field_230709_l_
        ) / 4F / CS;

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        // Move origin to center, scale and rotate
        RenderSystem.translated(cX, cY, this.func_230927_p_
                ());
        RenderSystem.scalef(scale, scale, 1);
        RenderSystem.rotatef((float) deg, 0, 0, 1);

        // Draw
        this.field_230706_i_
                .getTextureManager().bindTexture(centerTex);
        RenderSystem.color4f(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue(), BGT);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        builder.pos(CS / 2f, CS / 2f, this.func_230927_p_
                ()).tex(0.5f, 1).endVertex();
        builder.pos(CS / 2f, -CS / 2f, this.func_230927_p_
                ()).tex(0.5f, 0).endVertex();
        builder.pos(-CS / 2f, -CS / 2f, this.func_230927_p_
                ()).tex(0, 0).endVertex();
        builder.pos(-CS / 2f, CS / 2f, this.func_230927_p_
                ()).tex(0, 1).endVertex();
        tessellator.draw();


        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    private void drawUnselectedCenter(double cX, double cY) {

        float scale = (this.field_230709_l_
        ) / 4F / CS;

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        // Move origin to center, scale and rotate
        RenderSystem.translated(cX, cY, this.func_230927_p_
                ());
        RenderSystem.scalef(scale, scale, 1);

        // Draw
        this.field_230706_i_
                .getTextureManager().bindTexture(centerTex);
        RenderSystem.color4f(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue(), BGT);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        builder.pos(CS / 2f, CS / 2f, this.func_230927_p_
                ()).tex(1, 1).endVertex();
        builder.pos(CS / 2f, -CS / 2f, this.func_230927_p_
                ()).tex(1, 0).endVertex();
        builder.pos(-CS / 2f, -CS / 2f, this.func_230927_p_
                ()).tex(0.5f, 0).endVertex();
        builder.pos(-CS / 2f, CS / 2f, this.func_230927_p_
                ()).tex(0.5f, 1).endVertex();
        tessellator.draw();


        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    /**
     * Calculates the absolute mouse coordinates from the scaled ones and sets the cursor accordingly
     *
     * @param x
     * @param y
     */
    private void setAbsoluteMouse(double x, double y) {
        x = x * this.field_230706_i_
                .getMainWindow().getFramebufferWidth() / this.field_230708_k_
        ;
        y = -(y + 1 - field_230709_l_
        ) * this.field_230706_i_
                .getMainWindow().getFramebufferHeight() / field_230709_l_
        ;
        GLFW.glfwSetCursorPos(this.field_230706_i_
                .getMainWindow().getHandle(), x, y);
    }

    /**
     * Checks if the mouse if the mouse cursor is to far from the center and moves it back if necessary
     *
     * @param x  MouseX
     * @param y  MouseY
     * @param cX CenterX
     * @param cY CenterY
     * @param r  Allowed distance
     * @return Angle/Direction of the mouse pointer as seen from the center
     */
    private double updateMouse(int x, int y, int cX, int cY, double r) {
        int dx = (x - cX);
        int dy = (y - cY);
        double rad = (Math.atan2(dy, -dx) + Math.PI);

        if (Math.abs(dx) > Math.abs(Math.cos(rad) * r) + 8 || Math.abs(dy) > Math.abs(Math.sin(rad) * r) + 8) {
            setAbsoluteMouse(dx / 1.5 + cX + 4, cY - dy / 1.5);
        }
        return rad;
    }
}