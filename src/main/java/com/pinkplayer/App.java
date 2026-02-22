package com.pinkplayer;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

public class App extends Application {
    private MediaPlayer mediaPlayer;
    private java.lang.Boolean userSeeking = false;

    @Override
    public void start(Stage stage) {
        Label title = new Label("Pink Music Player");
        title.getStyleClass().addAll("title");

        Label subtitle = new Label("Pick a song from your computer and press play.");
        subtitle.getStyleClass().addAll("subtitle");

        Label status = new Label("Status: no track loaded");
        status.getStyleClass().addAll("status");

        Button chooseButton = new Button("Choose Song");
        chooseButton.getStyleClass().addAll("primary");

        Label trackLabel = new Label("No file selected");
        trackLabel.getStyleClass().addAll("track");

        Slider progressSlider = new Slider(0, 1, 0);
        progressSlider.setDisable(true);
        progressSlider.setMaxWidth(Double.MAX_VALUE);

        Label timeLabel = new Label("00:00 / --:--");
        timeLabel.getStyleClass().addAll("time");

        Button playButton = new Button("Play");
        playButton.getStyleClass().addAll("primary");

        Button pauseButton = new Button("Pause");
        Button stopButton = new Button("Stop");

        Slider volumeSlider = new Slider(0, 1, 0.75);
        volumeSlider.setMaxWidth(Double.MAX_VALUE);

        Label volumeLabel = new Label("Volume");
        volumeLabel.getStyleClass().addAll("field-label");

        Label hint = new Label("Supports common audio types like MP3, WAV, and M4A (depends on your OS).");
        hint.getStyleClass().addAll("hint");

        chooseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose an audio file");
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(
                    "Audio Files", "*.mp3", "*.wav", "*.m4a", "*.aac"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                loadMedia(file, status, trackLabel, progressSlider, timeLabel, volumeSlider);
            }
        });

        playButton.setOnAction(event -> {
            if (mediaPlayer == null) {
                status.setText("Status: choose a file first");
                return;
            }
            mediaPlayer.play();
            status.setText("Status: playing");
        });

        pauseButton.setOnAction(event -> {
            if (mediaPlayer == null) {
                status.setText("Status: choose a file first");
                return;
            }
            mediaPlayer.pause();
            status.setText("Status: paused");
        });

        stopButton.setOnAction(event -> {
            if (mediaPlayer == null) {
                status.setText("Status: choose a file first");
                return;
            }
            mediaPlayer.stop();
            status.setText("Status: stopped");
        });

        progressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            userSeeking = isChanging;
            if (!(isChanging && mediaPlayer != null)) {
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
            }
        });
        progressSlider.setOnMouseReleased(event -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
            }
        });

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

        HBox chooseRow = new HBox(12, chooseButton, trackLabel);
        chooseRow.setAlignment(Pos.CENTER_LEFT);

        HBox progressRow = new HBox(12, progressSlider, timeLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        HBox controlsRow = new HBox(10, playButton, pauseButton, stopButton);
        controlsRow.setAlignment(Pos.CENTER_LEFT);

        HBox volumeRow = new HBox(10, volumeLabel, volumeSlider);
        volumeRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14,
                title,
                subtitle,
                status,
                chooseRow,
                progressRow,
                controlsRow,
                volumeRow,
                hint);
        card.getStyleClass().addAll("card");
        card.setMaxWidth(560);

        StackPane root = new StackPane();
        root.getStyleClass().addAll("root-pane");
        root.getChildren().addAll(buildBubbles(), card);

        StackPane.setAlignment(card, Pos.CENTER);
        StackPane.setMargin(card, new Insets(24));

        Scene scene = new Scene(root, 820, 520);
        scene.getStylesheets().addAll(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("Pink Music Player");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        disposePlayer();
    }

    private void loadMedia(File file, Label status, Label trackLabel, Slider progressSlider, Label timeLabel, Slider volumeSlider) {
        disposePlayer();

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volumeSlider.getValue());

            trackLabel.setText(file.getName());
            status.setText("Status: loading track...");
            progressSlider.setDisable(true);
            progressSlider.setValue(0);
            timeLabel.setText("00:00 / --:--");

            media.setOnError(() -> status.setText("Status: could not read this audio file"));
            mediaPlayer.setOnError(() -> status.setText("Status: playback error"));

            mediaPlayer.setOnReady(() -> {
                Duration total = media.getDuration();
                if (total != null && !total.isUnknown()) {
                    progressSlider.setMax(Math.max(1, total.toSeconds()));
                }
                progressSlider.setDisable(false);
                updateTimeLabel(Duration.ZERO, total, timeLabel);
                status.setText("Status: ready to play");
            });

            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!userSeeking) {
                    progressSlider.setValue(newTime.toSeconds());
                }
                updateTimeLabel(newTime, media.getDuration(), timeLabel);
            });

            mediaPlayer.setOnEndOfMedia(() -> status.setText("Status: finished"));
        } catch (Exception ex) {
            status.setText("Status: could not load file");
        }
    }

    private void disposePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    private void updateTimeLabel(Duration current, Duration total, Label label) {
        String currentText = formatTime(current);
        String totalText = (total == null || total.isUnknown()) ? "--:--" : formatTime(total);
        label.setText(currentText + " / " + totalText);
    }

    private String formatTime(Duration duration) {
        if (duration == null || duration.isUnknown()) {
            return "00:00";
        }

        long totalSeconds = (long) Math.floor(duration.toSeconds());
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private StackPane buildBubbles() {
        Circle c1 = new Circle(140, Color.web("#ffd1e8", 0.45));
        c1.setTranslateX(-260);
        c1.setTranslateY(-140);

        Circle c2 = new Circle(90, Color.web("#ffc0da", 0.35));
        c2.setTranslateX(260);
        c2.setTranslateY(-120);

        Circle c3 = new Circle(120, Color.web("#ffe0ef", 0.55));
        c3.setTranslateX(240);
        c3.setTranslateY(180);

        Circle c4 = new Circle(70, Color.web("#ffb8d6", 0.4));
        c4.setTranslateX(-220);
        c4.setTranslateY(170);

        StackPane bubbleLayer = new StackPane(c1, c2, c3, c4);
        bubbleLayer.setMouseTransparent(true);
        return bubbleLayer;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
