package zk.planet_generator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * Planet shader code: https://gamedev.stackexchange.com/questions/9346/2d-shader-to-draw-representation-of-rotating-sphere
 * Noise generator code: http://devmag.org.za/2009/04/25/perlin-noise/
 */
public class PlanetGenerator extends ApplicationAdapter {
    public static final int BUFFER_WIDTH = 640;
    public static final int BUFFER_HEIGHT = 360;
    public static final int CENTER_X = BUFFER_WIDTH / 2;
    public static final int CENTER_Y = BUFFER_HEIGHT / 2;

    public static ShaderProgram planetShader;

    private OrthographicCamera gameCamera;
    private OrthographicCamera camera;

    private SpriteBatch batch;
    private PixelBuffer pixelBuffer;

    private Array<Star> stars;
    private Array<SpaceObject> spaceObjects;

    @Override
    public void create() {
        setupRendering();
        generateObjects();
        initializeInput();
    }

    private void setupRendering() {
        ShaderProgram.pedantic = false;
        planetShader = new ShaderProgram(Gdx.files.internal("shaders/planet.vsh"), Gdx.files.internal("shaders/planet.fsh"));
        if(!planetShader.isCompiled()) {
            Gdx.app.error("Planet Shader", "\n" + planetShader.getLog());
        }

        gameCamera = new OrthographicCamera();
        gameCamera.setToOrtho(false, BUFFER_WIDTH, BUFFER_HEIGHT);
        gameCamera.update();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        batch = new SpriteBatch();
        pixelBuffer = new PixelBuffer();
    }

    private void generateObjects() {
        spaceObjects = new Array<SpaceObject>();

        generateRandomPlanet();

        Orbiter.OrbiterBlueprint moonBlueprint = new Orbiter.OrbiterBlueprint();
        moonBlueprint.angularVelocity = 50;
        moonBlueprint.zTilt = 35;
        moonBlueprint.xTilt = -20;
        moonBlueprint.angle = 86;
        moonBlueprint.radius = 250;
        spaceObjects.add(new Orbiter(createMoon(Color.rgba8888(176f / 255f, 155f / 255f, 178f / 255f, 1f), 32), moonBlueprint));

        Orbiter.OrbiterBlueprint moonBlueprint2 = new Orbiter.OrbiterBlueprint();
        moonBlueprint2.angularVelocity = 20;
        moonBlueprint2.zTilt = 10;
        moonBlueprint2.xTilt = -20;
        moonBlueprint2.angle = 0;
        moonBlueprint2.radius = 300;
        spaceObjects.add(new Orbiter(createMoon(Color.rgba8888(156f / 255f, 155f / 255f, 190f / 255f, 1f), 20), moonBlueprint2));

        ColorGroup inner = new ColorGroup()
                .add(Color.rgba8888(223f / 255f, 233f / 255f, 180f / 255f, 1f))
                .add(Color.rgba8888(170f / 255f, 90f / 255f, 103f / 255f, 1f));

        for(int i = 0; i < 250; i++) {
            Orbiter.OrbiterBlueprint blueprint = new Orbiter.OrbiterBlueprint();
            blueprint.angularVelocity = 25;
            blueprint.zTilt = 15;
            blueprint.xTilt = -25;
            blueprint.angle = MathUtils.random(0, 360);
            blueprint.radius = MathUtils.random(80, 120);

            if (MathUtils.randomBoolean(0.9f)) {
                spaceObjects.add(new Orbiter(createMoon(inner.random(), MathUtils.random(4, 6)), blueprint));
            } else {
                spaceObjects.add(new Orbiter(createSquare(inner.random(), MathUtils.random(4, 5)), blueprint));
            }
        }

        ColorGroup outer = new ColorGroup()
                .add(Color.rgba8888(191f / 255f, 231f / 255f, 231f / 255f, 1f))
                .add(Color.rgba8888(75f / 255f, 109f / 255f, 133f / 255f, 1f));

        for(int i = 0; i < 250; i++) {
            Orbiter.OrbiterBlueprint blueprint = new Orbiter.OrbiterBlueprint();
            blueprint.angularVelocity = 15;
            blueprint.zTilt = 15;
            blueprint.xTilt = -25;
            blueprint.angle = MathUtils.random(0, 360);
            blueprint.radius = MathUtils.random(130, 155);

            if (MathUtils.randomBoolean(0.9f)) {
                spaceObjects.add(new Orbiter(createMoon(outer.random(), MathUtils.random(4, 6)), blueprint));
            } else {
                spaceObjects.add(new Orbiter(createSquare(outer.random(), MathUtils.random(4, 5)), blueprint));
            }
        }

        stars = new Array<Star>();
        float starAmount  = MathUtils.random(20, 100);
        Texture pixelTexture = new Texture(Gdx.files.internal("pixel.png"));
        for(int i = 0; i < starAmount; i++) {
            Sprite star = new Sprite(pixelTexture);
            star.setPosition(MathUtils.random(0, BUFFER_WIDTH), MathUtils.random(0, BUFFER_HEIGHT));
            star.setColor(Color.WHITE);

            if(MathUtils.randomBoolean(0.1f)) {
                star.setSize(2, 2);
            }

            stars.add(new Star(star));
        }
    }

    private void initializeInput() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            public boolean keyDown (int keycode) {
                switch(keycode) {
                    case Input.Keys.ESCAPE:
                        Gdx.app.exit();
                        return true;

                    case Input.Keys.R:
                        reset();
                        return true;
                }

                return false;
            }
        });
    }

    private Sprite generatePlanetSprite(int size) {
        return new Sprite(generatePlanetTexture(size));
    }

    private void generateRandomPlanet() {
        Sprite planet = generatePlanetSprite(1024);
        planet.setSize(128, 128);
        planet.setPosition(CENTER_X - planet.getWidth() / 2, CENTER_Y - planet.getHeight() / 2);
        spaceObjects.add(new Planet(planet));
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        if(Gdx.input.isKeyPressed(Input.Keys.E)) {
            delta *= 5;
        }

        for(SpaceObject spaceObject : spaceObjects) {
            spaceObject.update(delta);
        }
        spaceObjects.sort();

        pixelBuffer.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        //Gdx.gl.glClearColor(25f / 255f, 20f / 255f, 30f / 255f, 1);
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1);

        batch.setProjectionMatrix(gameCamera.combined);
        batch.begin();
        for(Star star : stars) {
            //star.update(delta);
            star.render(batch);
        }

        for(SpaceObject spaceObject : spaceObjects) {
            spaceObject.render(batch);
        }
        batch.end();
        pixelBuffer.end();

        batch.setShader(null);
        pixelBuffer.render(batch, camera);
    }

    @Override
    public void dispose() {
        batch.dispose();
        pixelBuffer.dispose();
        planetShader.dispose();
    }

    private Texture generatePlanetTexture(int size) {
        float[][] generated = NoiseGenerator.GenerateWhiteNoise(size, size);
        generated = NoiseGenerator.GeneratePerlinNoise(generated, 8);

        Pixmap pixmap = new Pixmap(generated.length, generated.length, Pixmap.Format.RGBA8888);
        for (int x = 0; x < generated.length; x++) {
            for (int y = 0; y < generated.length; y++) {
                double value = generated[x][y];

                if(value < 0.40f) {
                    // Deep ocean
                    pixmap.drawPixel(x, y, Color.rgba8888(47f / 255f, 86f / 255f, 118f / 255f, 1f));
                } else if (value < 0.55f) {
                    // Ocean
                    pixmap.drawPixel(x, y, Color.rgba8888(62f / 255f, 120f / 255f, 160f / 255f, 1f));
                } else {
                    // Land
                    pixmap.drawPixel(x, y, Color.rgba8888(146f / 255f, 209f / 255f, 135f / 255f, 1f));
                }
            }
        }

        Texture planetTexture = new Texture(pixmap);
        pixmap.dispose();
        return planetTexture;
    }

    private Sprite createMoon(int color, int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillCircle(pixmap.getWidth()/2, pixmap.getHeight()/2, size / 2 - 1);
        Sprite sprite = new Sprite(new Texture(pixmap));
        pixmap.dispose();
        return sprite;
    }

    private Sprite createSquare(int color, int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillRectangle(0, 0, size, size);
        Sprite sprite = new Sprite(new Texture(pixmap));
        pixmap.dispose();
        return sprite;
    }

    public void reset() {
        spaceObjects.clear();
        stars.clear();
        generateObjects();
    }
}