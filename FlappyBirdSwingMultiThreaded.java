import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class FlappyBirdSwingMultiThreaded extends JPanel
        implements KeyListener {

    // =========================================================
    // WINDOW
    // =========================================================

    private static final int WIDTH = 400;
    private static final int HEIGHT = 600;


    // =========================================================
    // BIRD
    // =========================================================

    private static final int BIRD_SIZE = 30;
    private static final int BIRD_X = 90;

    private int birdY = HEIGHT / 2;
    private int velocity = 0;

    private static final int GRAVITY = 1;
    private static final int FLAP_POWER = -10;

    private int birdFrame = 0;


    // =========================================================
    // PIPES
    // =========================================================

    private static final int PIPE_WIDTH = 60;

    private int pipeGap = 155;

    private static final int BASE_PIPE_SPEED = 4;


    // =========================================================
    // COINS
    // =========================================================

    private static final int COIN_SIZE = 20;

    private int totalCoins = 0;


    // =========================================================
    // LIVES
    // =========================================================

    private static final int START_LIVES = 3;

    private int lives = START_LIVES;


    // =========================================================
    // GAME STATE
    // =========================================================

    private enum GameState {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER,
        LEADERBOARD,
        SETTINGS,
        STATS
    }

    private volatile GameState gameState =
            GameState.MENU;


    // =========================================================
    // SCORE
    // =========================================================

    private int score = 0;
    private int highScore = 0;

    private int pipesPassed = 0;


    // =========================================================
    // LEVEL
    // =========================================================

    private int level = 1;


    // =========================================================
    // COMBO
    // =========================================================

    private int combo = 0;

    private long lastComboTime = 0;


    // =========================================================
    // POWER UPS
    // =========================================================

    private enum PowerType {
        SHIELD,
        DOUBLE_SCORE,
        SLOW_MOTION,
        EXTRA_LIFE
    }

    private PowerUp activePowerUp = null;

    private long powerUpEndTime = 0;


    // =========================================================
    // INVULNERABILITY
    // =========================================================

    private long invulnerableUntil = 0;


    // =========================================================
    // GAME STATISTICS
    // =========================================================

    private int gamesPlayed = 0;

    private int bestLevel = 1;

    private int totalPipes = 0;


    // =========================================================
    // FPS
    // =========================================================

    private int fps = 0;

    private int frameCounter = 0;

    private long fpsStartTime =
            System.currentTimeMillis();


    // =========================================================
    // SETTINGS
    // =========================================================

    private boolean soundEnabled = true;


    // =========================================================
    // THREADS
    // =========================================================

    private Thread birdThread;
    private Thread pipeThread;
    private Thread coinThread;
    private Thread powerThread;
    private Thread repaintThread;


    // =========================================================
    // THREAD CONTROL
    // =========================================================

    private volatile boolean running = false;

    private volatile long sessionId = 0;


    // =========================================================
    // GAME OBJECT LISTS
    // =========================================================

    private final List<Pipe> pipes =
            Collections.synchronizedList(
                    new ArrayList<Pipe>()
            );

    private final List<Coin> coins =
            Collections.synchronizedList(
                    new ArrayList<Coin>()
            );

    private final List<PowerUp> powerUps =
            Collections.synchronizedList(
                    new ArrayList<PowerUp>()
            );


    // =========================================================
    // RANDOM
    // =========================================================

    private final Random random =
            new Random();


    // =========================================================
    // BUTTONS
    // =========================================================

    private JButton startButton;
    private JButton leaderboardButton;
    private JButton settingsButton;
    private JButton statsButton;
    private JButton exitButton;

    private JButton restartButton;
    private JButton menuButton;

    private JButton resumeButton;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public FlappyBirdSwingMultiThreaded() {

        setPreferredSize(
                new Dimension(WIDTH, HEIGHT)
        );

        setFocusable(true);

        addKeyListener(this);

        setLayout(null);

        loadData();

        createButtons();

        showMenu();
    }


    // =========================================================
    // CREATE BUTTONS
    // =========================================================

    private void createButtons() {

        // START
        startButton =
                createButton("PLAY GAME");

        startButton.setBounds(
                120, 300, 160, 40
        );

        startButton.addActionListener(
                e -> startNewGame()
        );

        add(startButton);


        // LEADERBOARD
        leaderboardButton =
                createButton("LEADERBOARD");

        leaderboardButton.setBounds(
                120, 350, 160, 40
        );

        leaderboardButton.addActionListener(
                e -> showLeaderboard()
        );

        add(leaderboardButton);


        // SETTINGS
        settingsButton =
                createButton("SETTINGS");

        settingsButton.setBounds(
                120, 400, 160, 40
        );

        settingsButton.addActionListener(
                e -> showSettings()
        );

        add(settingsButton);


        // STATS
        statsButton =
                createButton("STATISTICS");

        statsButton.setBounds(
                120, 450, 160, 40
        );

        statsButton.addActionListener(
                e -> showStats()
        );

        add(statsButton);


        // EXIT
        exitButton =
                createButton("EXIT");

        exitButton.setBounds(
                120, 500, 160, 40
        );

        exitButton.addActionListener(
                e -> System.exit(0)
        );

        add(exitButton);


        // RESTART
        restartButton =
                createButton("RESTART");

        restartButton.setBounds(
                90, 390, 100, 40
        );

        restartButton.addActionListener(
                e -> startNewGame()
        );

        add(restartButton);


        // MENU
        menuButton =
                createButton("MENU");

        menuButton.setBounds(
                210, 390, 100, 40
        );

        menuButton.addActionListener(
                e -> showMenu()
        );

        add(menuButton);


        // RESUME
        resumeButton =
                createButton("RESUME");

        resumeButton.setBounds(
                130, 360, 140, 40
        );

        resumeButton.addActionListener(
                e -> resumeGame()
        );

        add(resumeButton);
    }


    // =========================================================
    // BUTTON CREATOR
    // =========================================================

    private JButton createButton(String text) {

        JButton button =
                new JButton(text);

        button.setFocusable(false);

        button.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );

        return button;
    }


    // =========================================================
    // MENU
    // =========================================================

    private void showMenu() {

        stopThreads();

        gameState = GameState.MENU;

        startedReset();

        setMenuButtonsVisible();

        repaint();
    }


    // =========================================================
    // RESET BASIC GAME VALUES
    // =========================================================

    private void startedReset() {

        birdY = HEIGHT / 2;

        velocity = 0;

        score = 0;

        lives = START_LIVES;

        totalCoins = 0;

        pipesPassed = 0;

        level = 1;

        combo = 0;

        activePowerUp = null;

        pipes.clear();

        coins.clear();

        powerUps.clear();
    }


    // =========================================================
    // MENU BUTTON VISIBILITY
    // =========================================================

    private void setMenuButtonsVisible() {

        startButton.setVisible(true);
        leaderboardButton.setVisible(true);
        settingsButton.setVisible(true);
        statsButton.setVisible(true);
        exitButton.setVisible(true);

        restartButton.setVisible(false);
        menuButton.setVisible(false);
        resumeButton.setVisible(false);
    }


    // =========================================================
    // START GAME
    // =========================================================

    private void startNewGame() {

        stopThreads();

        sessionId++;

        startedReset();

        gameState =
                GameState.PLAYING;

        running = true;

        gamesPlayed++;

        saveData();

        addPipe();

        hideAllButtons();

        startThreads(sessionId);

        requestFocusInWindow();

        repaint();
    }


    // =========================================================
    // HIDE BUTTONS
    // =========================================================

    private void hideAllButtons() {

        startButton.setVisible(false);
        leaderboardButton.setVisible(false);
        settingsButton.setVisible(false);
        statsButton.setVisible(false);
        exitButton.setVisible(false);

        restartButton.setVisible(false);
        menuButton.setVisible(false);
        resumeButton.setVisible(false);
    }


    // =========================================================
    // START THREADS
    // =========================================================

    private void startThreads(long id) {

        birdThread =
                new Thread(
                        () -> runBird(id),
                        "Bird-Thread"
                );

        pipeThread =
                new Thread(
                        () -> runPipes(id),
                        "Pipe-Thread"
                );

        coinThread =
                new Thread(
                        () -> runCoins(id),
                        "Coin-Thread"
                );

        powerThread =
                new Thread(
                        () -> runPowerUps(id),
                        "PowerUp-Thread"
                );

        repaintThread =
                new Thread(
                        () -> runRepaint(id),
                        "Repaint-Thread"
                );


        birdThread.start();

        pipeThread.start();

        coinThread.start();

        powerThread.start();

        repaintThread.start();
    }


    // =========================================================
    // STOP THREADS
    // =========================================================

    private void stopThreads() {

        running = false;
    }


    // =========================================================
    // BIRD THREAD
    // =========================================================

    private void runBird(long id) {

        while (
                running &&
                sessionId == id
        ) {

            if (gameState ==
                    GameState.PLAYING) {

                velocity += GRAVITY;

                birdY += velocity;

                birdFrame++;

                if (birdY < 0) {

                    birdY = 0;

                    loseLife();
                }


                if (birdY + BIRD_SIZE >
                        HEIGHT - 30) {

                    birdY =
                            HEIGHT - 30 -
                            BIRD_SIZE;

                    loseLife();
                }


                checkPipeCollision();

                checkCoinCollision();

                checkPowerUpCollision();
            }

            sleep(20);
        }
    }


    // =========================================================
    // PIPE THREAD
    // =========================================================

    private void runPipes(long id) {

        while (
                running &&
                sessionId == id
        ) {

            if (gameState ==
                    GameState.PLAYING) {

                int speed =
                        getPipeSpeed();


                synchronized (pipes) {

                    Iterator<Pipe> iterator =
                            pipes.iterator();


                    while (iterator.hasNext()) {

                        Pipe pipe =
                                iterator.next();

                        int actualSpeed =
                                speed;


                        if (activePowerUp != null &&
                                activePowerUp.type ==
                                        PowerType.SLOW_MOTION) {

                            actualSpeed = 2;
                        }


                        pipe.x -= actualSpeed;


                        if (!pipe.passed &&
                                pipe.x + PIPE_WIDTH
                                        < BIRD_X) {

                            pipe.passed = true;

                            pipesPassed++;

                            totalPipes++;

                            addScore(1);

                            increaseCombo();
                        }


                        if (pipe.x + PIPE_WIDTH
                                < 0) {

                            iterator.remove();
                        }
                    }
                }


                synchronized (pipes) {

                    if (
                            pipes.isEmpty() ||
                            pipes.get(
                                    pipes.size() - 1
                            ).x < WIDTH - 200
                    ) {

                        addPipe();
                    }
                }
            }

            sleep(30);
        }
    }


    // =========================================================
    // COIN THREAD
    // =========================================================

    private void runCoins(long id) {

        while (
                running &&
                sessionId == id
        ) {

            if (gameState ==
                    GameState.PLAYING) {

                int speed =
                        getPipeSpeed();


                synchronized (coins) {

                    Iterator<Coin> iterator =
                            coins.iterator();


                    while (iterator.hasNext()) {

                        Coin coin =
                                iterator.next();

                        coin.x -= speed;


                        if (coin.x + COIN_SIZE < 0) {

                            iterator.remove();
                        }
                    }
                }


                synchronized (coins) {

                    if (
                            coins.size() < 4 &&
                            random.nextInt(100) < 4
                    ) {

                        addCoin();
                    }
                }
            }

            sleep(40);
        }
    }


    // =========================================================
    // POWER UP THREAD
    // =========================================================

    private void runPowerUps(long id) {

        while (
                running &&
                sessionId == id
        ) {

            if (gameState ==
                    GameState.PLAYING) {

                synchronized (powerUps) {

                    Iterator<PowerUp> iterator =
                            powerUps.iterator();


                    while (iterator.hasNext()) {

                        PowerUp power =
                                iterator.next();

                        power.x -=
                                getPipeSpeed();


                        if (power.x + 25 < 0) {

                            iterator.remove();
                        }
                    }
                }


                if (
                        powerUps.size() < 1 &&
                        random.nextInt(100) < 2
                ) {

                    addPowerUp();
                }


                updatePowerUp();
            }

            sleep(40);
        }
    }


    // =========================================================
    // REPAINT THREAD
    // =========================================================

    private void runRepaint(long id) {

        while (
                running &&
                sessionId == id
        ) {

            repaint();

            updateFPS();

            sleep(20);
        }
    }


    // =========================================================
    // ADD PIPE
    // =========================================================

    private void addPipe() {

        int minTop = 80;

        int maxTop =
                HEIGHT -
                pipeGap -
                100;


        int top =
                minTop +
                random.nextInt(
                        maxTop - minTop + 1
                );


        pipes.add(
                new Pipe(
                        WIDTH,
                        top
                )
        );
    }


    // =========================================================
    // ADD COIN
    // =========================================================

    private void addCoin() {

        int x =
                WIDTH +
                random.nextInt(100);


        int y =
                100 +
                random.nextInt(
                        HEIGHT - 220
                );


        coins.add(
                new Coin(
                        x,
                        y
                )
        );
    }


    // =========================================================
    // ADD POWER UP
    // =========================================================

    private void addPowerUp() {

        int x = WIDTH + 100;

        int y =
                100 +
                random.nextInt(
                        HEIGHT - 230
                );


        PowerType type =
                PowerType.values()[
                        random.nextInt(
                                PowerType.values().length
                        )
                ];


        powerUps.add(
                new PowerUp(
                        x,
                        y,
                        type
                )
        );
    }


    // =========================================================
    // SPEED / LEVEL
    // =========================================================

    private int getPipeSpeed() {

        level =
                Math.min(
                        8,
                        1 + score / 10
                );


        pipeGap =
                Math.max(
                        115,
                        155 - (level - 1) * 8
                );


        return Math.min(
                8,
                BASE_PIPE_SPEED +
                        (level - 1)
            );
    }


    // =========================================================
    // SCORE
    // =========================================================

    private void addScore(int amount) {

        if (
                activePowerUp != null &&
                activePowerUp.type ==
                        PowerType.DOUBLE_SCORE
        ) {

            amount *= 2;
        }


        score += amount;


        if (score > highScore) {

            highScore = score;

            saveData();
        }


        if (level > bestLevel) {

            bestLevel = level;

            saveData();
        }
    }


    // =========================================================
    // COMBO
    // =========================================================

    private void increaseCombo() {

        long now =
                System.currentTimeMillis();


        if (
                now - lastComboTime
                        < 3000
        ) {

            combo++;

        } else {

            combo = 1;
        }


        lastComboTime = now;


        if (combo >= 5) {

            addScore(5);

            combo = 0;
        }
    }


    // =========================================================
    // PIPE COLLISION
    // =========================================================

    private void checkPipeCollision() {

        if (
                System.currentTimeMillis()
                        < invulnerableUntil
        ) {

            return;
        }


        synchronized (pipes) {

            for (Pipe pipe : pipes) {

                boolean horizontal =
                        BIRD_X + BIRD_SIZE >
                                pipe.x &&
                        BIRD_X <
                                pipe.x +
                                PIPE_WIDTH;


                boolean vertical =
                        birdY <
                                pipe.topHeight ||
                        birdY + BIRD_SIZE >
                                pipe.topHeight +
                                pipeGap;


                if (
                        horizontal &&
                        vertical
                ) {

                    loseLife();

                    return;
                }
            }
        }
    }


    // =========================================================
    // COIN COLLISION
    // =========================================================

    private void checkCoinCollision() {

        synchronized (coins) {

            Iterator<Coin> iterator =
                    coins.iterator();


            while (iterator.hasNext()) {

                Coin coin =
                        iterator.next();


                boolean collision =
                        BIRD_X <
                                coin.x +
                                COIN_SIZE &&
                        BIRD_X +
                                BIRD_SIZE >
                                coin.x &&
                        birdY <
                                coin.y +
                                COIN_SIZE &&
                        birdY +
                                BIRD_SIZE >
                                coin.y;


                if (collision) {

                    totalCoins++;

                    addScore(5);

                    increaseCombo();

                    iterator.remove();

                    playBeep();
                }
            }
        }
    }


    // =========================================================
    // POWER UP COLLISION
    // =========================================================

    private void checkPowerUpCollision() {

        synchronized (powerUps) {

            Iterator<PowerUp> iterator =
                    powerUps.iterator();


            while (iterator.hasNext()) {

                PowerUp power =
                        iterator.next();


                boolean collision =
                        BIRD_X <
                                power.x + 25 &&
                        BIRD_X + BIRD_SIZE >
                                power.x &&
                        birdY <
                                power.y + 25 &&
                        birdY + BIRD_SIZE >
                                power.y;


                if (collision) {

                    activatePowerUp(
                            power.type
                    );

                    iterator.remove();

                    playBeep();
                }
            }
        }
    }


    // =========================================================
    // ACTIVATE POWER UP
    // =========================================================

    private void activatePowerUp(
            PowerType type) {

        activePowerUp =
                new PowerUp(
                        0,
                        0,
                        type
                );


        powerUpEndTime =
                System.currentTimeMillis()
                        + 6000;


        if (
                type ==
                        PowerType.EXTRA_LIFE
        ) {

            lives =
                    Math.min(
                            5,
                            lives + 1
                    );

            activePowerUp = null;
        }
    }


    // =========================================================
    // UPDATE POWER UP
    // =========================================================

    private void updatePowerUp() {

        if (
                activePowerUp != null &&
                System.currentTimeMillis()
                        > powerUpEndTime
        ) {

            activePowerUp = null;
        }
    }


    // =========================================================
    // LOSE LIFE
    // =========================================================

    private void loseLife() {

        long now =
                System.currentTimeMillis();


        if (
                now < invulnerableUntil
        ) {

            return;
        }


        /*
         * Shield prevents one collision.
         */
        if (
                activePowerUp != null &&
                activePowerUp.type ==
                        PowerType.SHIELD
        ) {

            activePowerUp = null;

            invulnerableUntil =
                    now + 1200;

            return;
        }


        lives--;

        combo = 0;

        invulnerableUntil =
                now + 1200;


        birdY = HEIGHT / 2;

        velocity = 0;


        playBeep();


        if (lives <= 0) {

            gameOver();
        }
    }


    // =========================================================
    // GAME OVER
    // =========================================================

    private void gameOver() {

        gameState =
                GameState.GAME_OVER;

        running = false;


        if (score > highScore) {

            highScore = score;
        }


        saveData();


        SwingUtilities.invokeLater(
                () -> {

                    restartButton.setVisible(true);

                    menuButton.setVisible(true);

                    repaint();
                }
        );
    }


    // =========================================================
    // PAUSE
    // =========================================================

    private void togglePause() {

        if (
                gameState ==
                        GameState.PLAYING
        ) {

            gameState =
                    GameState.PAUSED;

            resumeButton.setVisible(true);

            menuButton.setVisible(true);

            repaint();

        } else if (
                gameState ==
                        GameState.PAUSED
        ) {

            resumeGame();
        }
    }


    // =========================================================
    // RESUME
    // =========================================================

    private void resumeGame() {

        gameState =
                GameState.PLAYING;

        resumeButton.setVisible(false);

        menuButton.setVisible(false);

        requestFocusInWindow();

        repaint();
    }


    // =========================================================
    // LEADERBOARD
    // =========================================================

    private void showLeaderboard() {

        stopThreads();

        gameState =
                GameState.LEADERBOARD;

        hideAllButtons();

        menuButton.setVisible(true);

        repaint();
    }


    // =========================================================
    // SETTINGS
    // =========================================================

    private void showSettings() {

        stopThreads();

        gameState =
                GameState.SETTINGS;

        hideAllButtons();

        menuButton.setVisible(true);

        repaint();
    }


    // =========================================================
    // STATISTICS
    // =========================================================

    private void showStats() {

        stopThreads();

        gameState =
                GameState.STATS;

        hideAllButtons();

        menuButton.setVisible(true);

        repaint();
    }


    // =========================================================
    // FPS
    // =========================================================

    private void updateFPS() {

        frameCounter++;

        long now =
                System.currentTimeMillis();


        if (
                now - fpsStartTime >= 1000
        ) {

            fps = frameCounter;

            frameCounter = 0;

            fpsStartTime = now;
        }
    }


    // =========================================================
    // SOUND
    // =========================================================

    private void playBeep() {

        if (soundEnabled) {

            Toolkit.getDefaultToolkit().beep();
        }
    }


    // =========================================================
    // SAVE DATA
    // =========================================================

    private void saveData() {

        try {

            PrintWriter writer =
                    new PrintWriter(
                            new FileWriter(
                                    "game_stats.txt"
                            )
                    );


            writer.println(highScore);

            writer.println(gamesPlayed);

            writer.println(bestLevel);

            writer.println(totalPipes);


            writer.close();

        } catch (IOException e) {

            System.out.println(
                    "Could not save game data."
            );
        }
    }


    // =========================================================
    // LOAD DATA
    // =========================================================

    private void loadData() {

        File file =
                new File(
                        "game_stats.txt"
                );


        if (!file.exists()) {

            return;
        }


        try {

            Scanner scanner =
                    new Scanner(file);


            if (scanner.hasNextInt()) {

                highScore =
                        scanner.nextInt();
            }


            if (scanner.hasNextInt()) {

                gamesPlayed =
                        scanner.nextInt();
            }


            if (scanner.hasNextInt()) {

                bestLevel =
                        scanner.nextInt();
            }


            if (scanner.hasNextInt()) {

                totalPipes =
                        scanner.nextInt();
            }


            scanner.close();

        } catch (Exception e) {

            System.out.println(
                    "Could not load game data."
            );
        }
    }


    // =========================================================
    // PAINT
    // =========================================================

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 =
                (Graphics2D) g.create();


        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );


        if (
                gameState ==
                        GameState.MENU
        ) {

            drawMenu(g2);

        } else if (
                gameState ==
                        GameState.LEADERBOARD
        ) {

            drawLeaderboard(g2);

        } else if (
                gameState ==
                        GameState.SETTINGS
        ) {

            drawSettings(g2);

        } else if (
                gameState ==
                        GameState.STATS
        ) {

            drawStats(g2);

        } else {

            drawGame(g2);
        }


        g2.dispose();
    }


    // =========================================================
    // MENU DRAWING
    // =========================================================

    private void drawMenu(
            Graphics2D g2) {

        g2.setColor(
                new Color(
                        190,
                        225,
                        235
                )
        );

        g2.fillRect(
                0,
                0,
                WIDTH,
                HEIGHT
        );


        drawCentered(
                g2,
                "FLAPPY BIRD",
                120,
                38,
                new Color(
                        45,
                        55,
                        65
                )
        );


        drawCentered(
                g2,
                "ARCADE EDITION",
                155,
                18,
                new Color(
                        70,
                        80,
                        90
                )
        );


        // Bird
        drawBird(
                g2,
                175,
                205
        );


        drawCentered(
                g2,
                "Multithreaded Java Swing Game",
                270,
                14,
                new Color(
                        70,
                        80,
                        90
                )
        );
    }


    // =========================================================
    // GAME DRAWING
    // =========================================================

    private void drawGame(
            Graphics2D g2) {

        Color background;

        if (score < 10) {

            background =
                    new Color(
                            190,
                            225,
                            235
                    );

        } else if (score < 20) {

            background =
                    new Color(
                            245,
                            215,
                            180
                    );

        } else {

            background =
                    new Color(
                            40,
                            50,
                            70
                    );
        }


        // Background
        g2.setColor(background);

        g2.fillRect(
                0,
                0,
                WIDTH,
                HEIGHT
        );


        drawBackground(g2);

        drawPipes(g2);

        drawCoins(g2);

        drawPowerUps(g2);

        drawBird(
                g2,
                BIRD_X,
                birdY
        );


        // Ground
        g2.setColor(
                new Color(
                        75,
                        75,
                        75
                )
        );

        g2.fillRect(
                0,
                HEIGHT - 30,
                WIDTH,
                30
        );


        drawHUD(g2);


        if (
                gameState ==
                        GameState.PAUSED
        ) {

            drawOverlay(
                    g2,
                    "PAUSED",
                    "Press P to Resume"
            );
        }


        if (
                gameState ==
                        GameState.GAME_OVER
        ) {

            drawGameOver(g2);
        }
    }


    // =========================================================
    // BACKGROUND
    // =========================================================

    private void drawBackground(
            Graphics2D g2) {

        // Clouds
        g2.setColor(
                new Color(
                        255,
                        255,
                        255,
                        100
                )
        );


        int cloudOffset =
                (birdFrame / 2) % WIDTH;


        for (
                int x = -100;
                x < WIDTH + 100;
                x += 150
        ) {

            int cx =
                    x - cloudOffset;


            g2.fillOval(
                    cx,
                    90,
                    60,
                    25
            );

            g2.fillOval(
                    cx + 25,
                    78,
                    55,
                    35
            );
        }


        // Mountains
        g2.setColor(
                new Color(
                        120,
                        150,
                        145,
                        100
                )
        );


        int[] xPoints = {
                0,
                80,
                150,
                230,
                310,
                400
        };


        int[] yPoints = {
                420,
                330,
                410,
                300,
                400,
                340
        };


        g2.fillPolygon(
                xPoints,
                yPoints,
                xPoints.length
        );
    }


    // =========================================================
    // PIPES
    // =========================================================

    private void drawPipes(
            Graphics2D g2) {

        g2.setColor(
                new Color(
                        70,
                        140,
                        85
                )
        );


        synchronized (pipes) {

            for (Pipe pipe : pipes) {

                g2.fillRect(
                        pipe.x,
                        0,
                        PIPE_WIDTH,
                        pipe.topHeight
                );


                int bottom =
                        pipe.topHeight +
                        pipeGap;


                g2.fillRect(
                        pipe.x,
                        bottom,
                        PIPE_WIDTH,
                        HEIGHT -
                                30 -
                                bottom
                );
            }
        }
    }


    // =========================================================
    // COINS
    // =========================================================

    private void drawCoins(
            Graphics2D g2) {

        synchronized (coins) {

            for (Coin coin : coins) {

                g2.setColor(
                        new Color(
                                235,
                                190,
                                45
                        )
                );


                g2.fillOval(
                        coin.x,
                        coin.y,
                        COIN_SIZE,
                        COIN_SIZE
                );


                g2.setColor(
                        Color.WHITE
                );


                g2.drawString(
                        "+5",
                        coin.x - 1,
                        coin.y + 15
                );
            }
        }
    }


    // =========================================================
    // POWER UPS
    // =========================================================

    private void drawPowerUps(
            Graphics2D g2) {

        synchronized (powerUps) {

            for (
                    PowerUp power :
                    powerUps
            ) {

                if (
                        power.type ==
                                PowerType.SHIELD
                ) {

                    g2.setColor(
                            new Color(
                                    70,
                                    160,
                                    255
                            )
                    );

                } else if (
                        power.type ==
                                PowerType.DOUBLE_SCORE
                ) {

                    g2.setColor(
                            new Color(
                                    255,
                                    180,
                                    40
                            )
                    );

                } else if (
                        power.type ==
                                PowerType.SLOW_MOTION
                ) {

                    g2.setColor(
                            new Color(
                                    150,
                                    100,
                                    220
                            )
                    );

                } else {

                    g2.setColor(
                            new Color(
                                    220,
                                    70,
                                    80
                            )
                    );
                }


                g2.fillOval(
                        power.x,
                        power.y,
                        25,
                        25
                );


                g2.setColor(
                        Color.WHITE
                );


                String symbol = "?";


                if (
                        power.type ==
                                PowerType.SHIELD
                ) {

                    symbol = "S";

                } else if (
                        power.type ==
                                PowerType.DOUBLE_SCORE
                ) {

                    symbol = "2";

                } else if (
                        power.type ==
                                PowerType.SLOW_MOTION
                ) {

                    symbol = "T";

                } else {

                    symbol = "+";
                }


                g2.drawString(
                        symbol,
                        power.x + 8,
                        power.y + 17
                );
            }
        }
    }


    // =========================================================
    // BIRD
    // =========================================================

    private void drawBird(
            Graphics2D g2,
            int x,
            int y) {

        g2.setColor(
                new Color(
                        245,
                        180,
                        45
                )
        );


        g2.fillOval(
                x,
                y,
                BIRD_SIZE,
                BIRD_SIZE
        );


        // Wing animation
        g2.setColor(
                new Color(
                        220,
                        150,
                        35
                )
        );


        int wingY =
                y + 15 +
                (birdFrame / 5 % 2 == 0
                        ? -3 : 3);


        g2.fillOval(
                x + 5,
                wingY,
                15,
                10
        );


        // Eye
        g2.setColor(
                Color.WHITE
        );


        g2.fillOval(
                x + 19,
                y + 6,
                7,
                7
        );


        g2.setColor(
                Color.BLACK
        );


        g2.fillOval(
                x + 22,
                y + 8,
                3,
                3
        );


        // Beak
        g2.setColor(
                new Color(
                        230,
                        100,
                        40
                )
        );


        int[] xp = {
                x + 30,
                x + 39,
                x + 30
        };


        int[] yp = {
                y + 12,
                y + 16,
                y + 20
        };


        g2.fillPolygon(
                xp,
                yp,
                3
        );


        // Shield visual
        if (
                activePowerUp != null &&
                activePowerUp.type ==
                        PowerType.SHIELD
        ) {

            g2.setColor(
                    new Color(
                            70,
                            160,
                            255,
                            100
                    )
            );


            g2.drawOval(
                    x - 7,
                    y - 7,
                    44,
                    44
            );
        }
    }


    // =========================================================
    // HUD
    // =========================================================

    private void drawHUD(
            Graphics2D g2) {

        g2.setColor(
                Color.WHITE
        );


        g2.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        15
                )
        );


        g2.drawString(
                "SCORE",
                15,
                22
        );


        g2.drawString(
                "" + score,
                15,
                42
        );


        g2.drawString(
                "BEST",
                100,
                22
        );


        g2.drawString(
                "" + highScore,
                100,
                42
        );


        g2.drawString(
                "COINS",
                180,
                22
        );


        g2.drawString(
                "" + totalCoins,
                180,
                42
        );


        g2.drawString(
                "LEVEL",
                270,
                22
        );


        g2.drawString(
                "" + level,
                270,
                42
        );


        g2.drawString(
                "❤ " + lives,
                335,
                32
        );


        // Combo
        if (combo > 1) {

            drawCentered(
                    g2,
                    "COMBO x" + combo,
                    75,
                    18,
                    Color.WHITE
            );
        }


        // Active power
        if (activePowerUp != null) {

            long remaining =
                    Math.max(
                            0,
                            (powerUpEndTime -
                                    System.currentTimeMillis())
                                    / 1000
                    );


            String text =
                    getPowerName(
                            activePowerUp.type
                    ) +
                    "  " +
                    remaining +
                    "s";


            drawCentered(
                    g2,
                    text,
                    100,
                    14,
                    Color.WHITE
            );
        }


        // FPS
        g2.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        11
                )
        );


        g2.drawString(
                "FPS: " + fps,
                10,
                HEIGHT - 10
        );


        // Thread indicator
        g2.drawString(
                "Threads: 5",
                320,
                HEIGHT - 10
        );
    }


    // =========================================================
    // POWER NAME
    // =========================================================

    private String getPowerName(
            PowerType type) {

        if (
                type ==
                        PowerType.SHIELD
        ) {

            return "SHIELD";

        } else if (
                type ==
                        PowerType.DOUBLE_SCORE
        ) {

            return "2X SCORE";

        } else if (
                type ==
                        PowerType.SLOW_MOTION
        ) {

            return "SLOW MOTION";
        }


        return "EXTRA LIFE";
    }


    // =========================================================
    // GAME OVER
    // =========================================================

    private void drawGameOver(
            Graphics2D g2) {

        g2.setColor(
                new Color(
                        0,
                        0,
                        0,
                        170
                )
        );


        g2.fillRect(
                0,
                0,
                WIDTH,
                HEIGHT
        );


        drawCentered(
                g2,
                "GAME OVER",
                230,
                36,
                Color.WHITE
        );


        drawCentered(
                g2,
                "Score: " + score,
                275,
                22,
                Color.WHITE
        );


        drawCentered(
                g2,
                "Best: " + highScore,
                305,
                20,
                Color.WHITE
        );


        drawCentered(
                g2,
                "Coins: " + totalCoins,
                335,
                18,
                Color.WHITE
        );


        if (score >= highScore) {

            drawCentered(
                    g2,
                    "NEW HIGH SCORE!",
                    370,
                    18,
                    new Color(
                            245,
                            190,
                            45
                    )
            );
        }
    }


    // =========================================================
    // PAUSE OVERLAY
    // =========================================================

    private void drawOverlay(
            Graphics2D g2,
            String title,
            String subtitle) {

        g2.setColor(
                new Color(
                        0,
                        0,
                        0,
                        130
                )
        );


        g2.fillRect(
                0,
                0,
                WIDTH,
                HEIGHT
        );


        drawCentered(
                g2,
                title,
                280,
                35,
                Color.WHITE
        );


        drawCentered(
                g2,
                subtitle,
                320,
                17,
                Color.WHITE
        );
    }


    // =========================================================
    // LEADERBOARD
    // =========================================================

    private void drawLeaderboard(
            Graphics2D g2) {

        drawSimpleScreen(
                g2,
                "LEADERBOARD"
        );


        drawCentered(
                g2,
                "CURRENT BEST SCORE",
                210,
                18,
                Color.WHITE
        );


        drawCentered(
                g2,
                "🏆  " + highScore,
                260,
                32,
                new Color(
                        245,
                        190,
                        45
                )
        );


        drawCentered(
                g2,
                "More player records can be added later.",
                320,
                14,
                Color.WHITE
        );
    }


    // =========================================================
    // SETTINGS
    // =========================================================

    private void drawSettings(
            Graphics2D g2) {

        drawSimpleScreen(
                g2,
                "SETTINGS"
        );


        drawCentered(
                g2,
                "Sound: " +
                        (soundEnabled
                                ? "ON"
                                : "OFF"),
                230,
                22,
                Color.WHITE
        );


        drawCentered(
                g2,
                "Press S to toggle sound",
                280,
                15,
                Color.WHITE
        );


        drawCentered(
                g2,
                "Theme: AUTO",
                330,
                20,
                Color.WHITE
        );
    }


    // =========================================================
    // STATISTICS
    // =========================================================

    private void drawStats(
            Graphics2D g2) {

        drawSimpleScreen(
                g2,
                "PLAYER STATISTICS"
        );


        drawCentered(
                g2,
                "Games Played: " +
                        gamesPlayed,
                210,
                19,
                Color.WHITE
        );


        drawCentered(
                g2,
                "Highest Score: " +
                        highScore,
                250,
                19,
                Color.WHITE
        );


        drawCentered(
                g2,
                "Best Level: " +
                        bestLevel,
                290,
                19,
                Color.WHITE
        );


        drawCentered(
                g2,
                "Total Pipes: " +
                        totalPipes,
                330,
                19,
                Color.WHITE
        );


        drawCentered(
                g2,
                "Total Coins: " +
                        totalCoins,
                370,
                19,
                Color.WHITE
        );
    }


    // =========================================================
    // SIMPLE SCREEN
    // =========================================================

    private void drawSimpleScreen(
            Graphics2D g2,
            String title) {

        g2.setColor(
                new Color(
                        45,
                        55,
                        70
                )
        );


        g2.fillRect(
                0,
                0,
                WIDTH,
                HEIGHT
        );


        drawCentered(
                g2,
                title,
                120,
                32,
                Color.WHITE
        );
    }


    // =========================================================
    // CENTER TEXT
    // =========================================================

    private void drawCentered(
            Graphics2D g2,
            String text,
            int y,
            int size,
            Color color) {

        g2.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        size
                )
        );


        FontMetrics fm =
                g2.getFontMetrics();


        int x =
                (WIDTH -
                        fm.stringWidth(text))
                        / 2;


        g2.setColor(color);


        g2.drawString(
                text,
                x,
                y
        );
    }


    // =========================================================
    // KEYBOARD
    // =========================================================

    @Override
    public void keyPressed(
            KeyEvent e) {

        int key =
                e.getKeyCode();


        // SPACE / W
        if (
                key ==
                        KeyEvent.VK_SPACE ||
                key ==
                        KeyEvent.VK_W
        ) {

            if (
                    gameState ==
                            GameState.PLAYING
            ) {

                velocity =
                        FLAP_POWER;
            }
        }


        // P
        if (
                key ==
                        KeyEvent.VK_P
        ) {

            togglePause();
        }


        // R
        if (
                key ==
                        KeyEvent.VK_R
        ) {

            if (
                    gameState ==
                            GameState.GAME_OVER
            ) {

                startNewGame();
            }
        }


        // ESC
        if (
                key ==
                        KeyEvent.VK_ESCAPE
        ) {

            if (
                    gameState !=
                            GameState.MENU
            ) {

                showMenu();
            }
        }


        // S
        if (
                key ==
                        KeyEvent.VK_S
        ) {

            if (
                    gameState ==
                            GameState.SETTINGS
            ) {

                soundEnabled =
                        !soundEnabled;
            }
        }
    }


    @Override
    public void keyReleased(
            KeyEvent e) {
    }


    @Override
    public void keyTyped(
            KeyEvent e) {
    }


    // =========================================================
    // SLEEP
    // =========================================================

    private void sleep(int ms) {

        try {

            Thread.sleep(ms);

        } catch (
                InterruptedException e
        ) {

            Thread.currentThread()
                    .interrupt();
        }
    }


    // =========================================================
    // PIPE CLASS
    // =========================================================

    private static class Pipe {

        int x;

        int topHeight;

        boolean passed;


        Pipe(
                int x,
                int topHeight
        ) {

            this.x = x;

            this.topHeight =
                    topHeight;

            this.passed = false;
        }
    }


    // =========================================================
    // COIN CLASS
    // =========================================================

    private static class Coin {

        int x;

        int y;


        Coin(
                int x,
                int y
        ) {

            this.x = x;

            this.y = y;
        }
    }


    // =========================================================
    // POWER UP CLASS
    // =========================================================

    private static class PowerUp {

        int x;

        int y;

        PowerType type;


        PowerUp(
                int x,
                int y,
                PowerType type
        ) {

            this.x = x;

            this.y = y;

            this.type = type;
        }
    }


    // =========================================================
    // MAIN
    // =========================================================

    public static void main(
            String[] args) {

        SwingUtilities.invokeLater(
                () -> {

                    JFrame frame =
                            new JFrame(
                                    "Flappy Bird - Arcade Edition"
                            );


                    FlappyBirdSwingMultiThreaded game =
                            new FlappyBirdSwingMultiThreaded();


                    frame.setDefaultCloseOperation(
                            JFrame.EXIT_ON_CLOSE
                    );


                    frame.setResizable(false);


                    frame.add(game);


                    frame.pack();


                    frame.setLocationRelativeTo(
                            null
                    );


                    frame.setVisible(true);


                    game.requestFocusInWindow();
                }
        );
    }
}