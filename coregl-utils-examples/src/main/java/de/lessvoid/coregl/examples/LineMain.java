/**
 * Copyright (c) 2013, Jens Hohmuth
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.lessvoid.coregl.examples;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.Charset;

import de.lessvoid.coregl.ColorFormat;
import de.lessvoid.coregl.CoreFBO;
import de.lessvoid.coregl.CoreRender;
import de.lessvoid.coregl.CoreShader;
import de.lessvoid.coregl.CoreTexture2D;
import de.lessvoid.coregl.CoreVAO;
import de.lessvoid.coregl.CoreVAO.FloatType;
import de.lessvoid.coregl.CoreVBO;
import de.lessvoid.coregl.CoreVBO.DataType;
import de.lessvoid.coregl.CoreVBO.UsageType;
import de.lessvoid.coregl.ResizeFilter;
import de.lessvoid.coregl.Type;
import de.lessvoid.coregl.spi.CoreGL;
import de.lessvoid.coregl.spi.CoreSetup.RenderLoopCallback;
import de.lessvoid.math.Mat4;
import de.lessvoid.math.MatrixFactory;

public class LineMain implements RenderLoopCallback {

  private static final int WINDOW_WIDTH = 1024, WINDOW_HEIGHT = 768;

  private static final Mat4 ORTHO = MatrixFactory.createOrtho(0, WINDOW_WIDTH, WINDOW_HEIGHT, 0);

  private CoreRender coreRender;
  private CoreShader texture;
  private CoreShader lineShader1;
  private CoreShader lineShader2;
  private CoreVAO src;
  private CoreVBO<FloatBuffer> vbo;
  private CoreVBO<FloatBuffer> vboQuad;
  private CoreVBO<FloatBuffer> vboBackground;
  private CoreFBO fbo;
  private float totalTime;
  private CoreTexture2D fboTexture;
  private CoreShader backgroundShader;
  private float time;

  @Override
  public void init(final CoreGL gl) {
    coreRender = CoreRender.createCoreRender(gl);

    texture = CoreShader.createShaderWithVertexAttributes(gl, "aVertex", "aUV");
    texture.vertexShader("line/line-pass2.vs");
    texture.fragmentShader("line/line-pass2.fs");
    texture.link();

    lineShader1 = CoreShader.createShaderWithVertexAttributes(gl, "aVertex");
    lineShader1.vertexShader("line/line.vs");
    try {
      lineShader1.geometryShader("line/line.gs",
                                 stream("#version 150 core\n#define CAP_ROUND\n#define JOIN_NONE\n"),
                                 resource("line/line.gs"));
    } catch (final FileNotFoundException e) {
      System.err.println("error loading geometry shader: " + e.toString());
    }
    lineShader1.fragmentShader("line/line.fs",
                               stream("#version 150 core\n#define CAP_ROUND\n#define JOIN_NONE\n"),
                               resource("line/line.fs"));
    lineShader1.link();

    lineShader2 = CoreShader.createShaderWithVertexAttributes(gl, "aVertex");
    lineShader2.vertexShader("line/line.vs");
    try {
      lineShader2.geometryShader("line/line.gs",
                                 stream("#version 150 core\n#define CAP_BUTT\n#define JOIN_NONE\n"),
                                 resource("line/line.gs"));
    } catch (final FileNotFoundException e) {
      System.err.println("error loading geometry shader: " + e.toString());
    }
    lineShader2.fragmentShader("line/line.fs",
                               stream("#version 150 core\n#define CAP_BUTT\n#define JOIN_NONE\n"),
                               resource("line/line.fs"));
    lineShader2.link();

    src = CoreVAO.createCoreVAO(gl);
    src.bind();

    vbo = CoreVBO.createCoreVBO(gl, DataType.FLOAT, UsageType.DYNAMIC_DRAW, 2 * 5);
    vbo.bind();
    src.enableVertexAttribute(0);
    src.vertexAttribPointer(0, 2, FloatType.FLOAT, 2, 0);
    totalTime = 0;

    vboQuad = CoreVBO.createCoreVBO(gl, DataType.FLOAT, UsageType.DYNAMIC_DRAW, 4 * 4);
    vboBackground = CoreVBO.createCoreVBO(gl, DataType.FLOAT, UsageType.DYNAMIC_DRAW, 5 * 4);

    fbo = CoreFBO.createCoreFBO(gl);
    fbo.bindFramebuffer();

    fboTexture = CoreTexture2D
        .createEmptyTexture(gl, ColorFormat.Red, Type.UNSIGNED_BYTE, WINDOW_WIDTH, WINDOW_HEIGHT, ResizeFilter.Linear);
    fbo.attachTexture(fboTexture.getTextureId(), 0);

    backgroundShader = CoreShader.createShaderWithVertexAttributes(gl, "aVertex", "aColor");
    backgroundShader.vertexShader("background-gradient.vs");
    backgroundShader.fragmentShader("background-gradient.fs");
    backgroundShader.link();
  }

  @Override
  public boolean render(final CoreGL gl, final float deltaTime) {
    time += deltaTime;

    fbo.bindFramebuffer();
    gl.glViewport(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

    gl.glClearColor(0.f, 0.f, 0.f, 0.f);
    gl.glClear(gl.GL_COLOR_BUFFER_BIT());
    // glClearStencil(0);
    // glStencilFuncSeparate(GL_FRONT_AND_BACK, GL_EQUAL, 0, 0xff);
    // glStencilOpSeparate(GL_FRONT_AND_BACK, GL_KEEP, GL_KEEP, GL_INCR);
    // glStencilMask(0xFF);
    // glEnable(GL_STENCIL_TEST);

    gl.glEnable(gl.GL_BLEND());
    gl.glBlendEquationSeparate(gl.GL_MAX(), gl.GL_MAX());

    totalTime += deltaTime;
    final float w = 100.f;
    final float r = 2.f;

    lineShader1.activate();
    lineShader1.setUniformMatrix("uMvp", 4, MatrixFactory.createOrtho(0, 1024.f, 0, 768.f).toBuffer());
    lineShader1.setUniformf("lineColor", 1.f, 1.f, 1.f, (float) ((Math.sin(time / 1700.f) + 1.0) / 2.0));
    lineShader1.setUniformf("lineParameters", (2 * r + w), (2 * r + w) / 2.f, (2 * r + w) / 2.f - 2 * r, (2 * r));

    final FloatBuffer buffer = vbo.getBuffer();
    buffer.put(100.f);
    buffer.put(100.f);
    buffer.put(100.f);
    buffer.put(100.f);
    buffer.put(600.f);
    buffer.put(300.f);
    buffer.put(600.f + (float) Math.cos(totalTime / 1500.f) * 200.f);
    buffer.put(300.f + (float) Math.sin(totalTime / 1500.f) * 200.f);
    buffer.put(600.f + (float) Math.cos(totalTime / 1500.f) * 200.f);
    buffer.put(300.f + (float) Math.sin(totalTime / 1500.f) * 200.f);
    buffer.rewind();
    vbo.send();

    src.enableVertexAttribute(0);
    src.disableVertexAttribute(1);
    src.vertexAttribPointer(0, 2, FloatType.FLOAT, 2, 0);
    coreRender.renderLinesAdjacent(5);
    fbo.disable();

    // Render background
    gl.glDisable(gl.GL_BLEND());

    final FloatBuffer background = vboBackground.getBuffer();
    background.put(0.f);
    background.put(0.f);
    background.put(1.f);
    background.put(0.f);
    background.put(0.f);

    background.put(0.f);
    background.put(0.f + 768);
    background.put(0.0f);
    background.put(1.0f);
    background.put(0.0f);

    background.put(0.f + 1024);
    background.put(0.f);
    background.put(0.0f);
    background.put(0.0f);
    background.put(1.0f);

    background.put(0.f + 1024);
    background.put(0.f + 768);
    background.put(1.0f);
    background.put(1.0f);
    background.put(1.0f);
    background.rewind();

    vboBackground.send();

    src.enableVertexAttribute(0);
    src.vertexAttribPointer(0, 2, FloatType.FLOAT, 5, 0);
    src.enableVertexAttribute(1);
    src.vertexAttribPointer(1, 3, FloatType.FLOAT, 5, 2);

    gl.glViewport(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
    backgroundShader.activate();
    backgroundShader.setUniformMatrix("uMvp", 4, ORTHO.toBuffer());
    coreRender.renderTriangleStrip(4);

    // Render lines fbo

    gl.glEnable(gl.GL_BLEND());
    gl.glBlendFunc(gl.GL_SRC_ALPHA(), gl.GL_ONE_MINUS_SRC_ALPHA());
    gl.glBlendEquationSeparate(gl.GL_FUNC_ADD(), gl.GL_FUNC_ADD());

    final FloatBuffer quad = vboQuad.getBuffer();
    quad.put(0.f);
    quad.put(0.f);
    quad.put(0.f);
    quad.put(0.f);

    quad.put(0.f);
    quad.put(0.f + 768);
    quad.put(0.0f);
    quad.put(1.0f);

    quad.put(0.f + 1024);
    quad.put(0.f);
    quad.put(1.0f);
    quad.put(0.0f);

    quad.put(0.f + 1024);
    quad.put(0.f + 768);
    quad.put(1.0f);
    quad.put(1.0f);
    quad.rewind();

    vboQuad.send();

    src.enableVertexAttribute(0);
    src.vertexAttribPointer(0, 2, FloatType.FLOAT, 4, 0);
    src.enableVertexAttribute(1);
    src.vertexAttribPointer(1, 2, FloatType.FLOAT, 4, 2);

    fboTexture.bind();
    texture.activate();
    texture.setUniformMatrix("uMvp", 4, ORTHO.toBuffer());
    texture.setUniformi("uTexture", 0);
    texture.setUniformf("lineColor", 1.f, 1.f, 1.f, 1.f);
    coreRender.renderTriangleStrip(4);
    return true;
  }

  @Override
  public boolean endLoop() {
    return false;
  }

  private InputStream resource(final String name) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
  }

  private InputStream stream(final String data) {
    return new ByteArrayInputStream(data.getBytes(Charset.forName("ISO-8859-1")));
  }

  public static void main(final String[] args) throws Exception {
    final RenderLoopCallback lineExample = new LineMain();
    CoreExampleMain.runExample(lineExample, args);
  }
}
