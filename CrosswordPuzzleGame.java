import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.imageio.*;
import javax.swing.*;

// Trie Node for storing words
class TrieNode {
    HashMap<Character, TrieNode> children = new HashMap<>();
    boolean isEndOfWord = false;
}

// Trie data structure for word validation
class Trie {
    private final TrieNode root = new TrieNode();

    public void insert(String word) {
        TrieNode current = root;
        for (char ch : word.toCharArray()) {
            current = current.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        current.isEndOfWord = true;
    }

    public boolean search(String word) {
        TrieNode current = root;
        for (char ch : word.toCharArray()) {
            current = current.children.get(ch);
            if (current == null) return false;
        }
        return current.isEndOfWord;
    }
}

// Crossword class for managing the puzzle grid and word placements
class Crossword {
    public final char[][] grid;
    private final Trie trie;

    public Crossword(int rows, int cols, Trie trie) {
        grid = new char[rows][cols];
        this.trie = trie;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = '-';
            }
        }
    }

    public boolean placeWord(String word, int row, int col, boolean horizontal) {
        if (canPlaceWord(word, row, col, horizontal)) {
            for (int i = 0; i < word.length(); i++) {
                if (horizontal) grid[row][col + i] = word.charAt(i);
                else grid[row + i][col] = word.charAt(i);
            }
            return true;
        }
        return false;
    }

    private boolean canPlaceWord(String word, int row, int col, boolean horizontal) {
        if (horizontal && col + word.length() > grid[0].length) return false;
        if (!horizontal && row + word.length() > grid.length) return false;

        for (int i = 0; i < word.length(); i++) {
            char cell = horizontal ? grid[row][col + i] : grid[row + i][col];
            if (cell != '-' && cell != word.charAt(i)) return false;
        }
        return true;
    }

    public boolean placeWordAutomatically(String word) {
        Random random = new Random();
        boolean horizontal = random.nextBoolean();
        for (int attempt = 0; attempt < 100; attempt++) {
            int row = random.nextInt(grid.length - (horizontal ? 0 : word.length()));
            int col = random.nextInt(grid[0].length - (horizontal ? word.length() : 0));
            if (placeWord(word, row, col, horizontal)) {
                return true;
            }
        }
        return false;
    }

    public void resetGrid() {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                grid[i][j] = '-';
            }
        }
    }
}

// Custom JPanel to set a background image
class ImagePanel extends JPanel {
    private final Image backgroundImage;

    public ImagePanel(Image backgroundImage) {
        this.backgroundImage = backgroundImage;
        setLayout(new OverlayLayout(this));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}

// Crossword UI class for displaying the puzzle and handling user interactions
class CrosswordUI extends JFrame {
    private final JTextField[][] gridFields;
    private final Crossword crossword;
    private final Trie trie;
    private final JLabel scoreLabel;
    private final JTextArea wordList;
    private int score = 0;
    private final Map<String, String> questions;
    private int questionIndex = 0;
    private final String[] questionKeys;

    public CrosswordUI(int rows, int cols, Trie trie, Crossword crossword, Image backgroundImage, Map<String, String> questions) {
        this.trie = trie;
        this.crossword = crossword;
        this.questions = questions;
        this.questionKeys = questions.keySet().toArray(new String[0]);
        this.gridFields = new JTextField[rows][cols];

        setTitle("Crossword Puzzle");
        setLayout(new BorderLayout());
        setBackground(Color.LIGHT_GRAY);

        ImagePanel gridPanel = new ImagePanel(backgroundImage);
        gridPanel.setLayout(new GridLayout(rows, cols));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Define font
        Font gridFont = new Font("Arial", Font.BOLD, 18); // Increased font size
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                gridFields[i][j] = new JTextField(1);
                gridFields[i][j].setEditable(false);
                gridFields[i][j].setHorizontalAlignment(JTextField.CENTER);
                gridFields[i][j].setOpaque(false);
                gridFields[i][j].setFont(gridFont); // Set font to bold and increased size
                gridFields[i][j].setForeground(Color.WHITE); // Set text color to white
                gridPanel.add(gridFields[i][j]);
            }
        }
        add(gridPanel, BorderLayout.CENTER);

        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(Color.LIGHT_GRAY);

        // Word list area
        wordList = new JTextArea(5, 10);
        wordList.setEditable(false);
        wordList.setFont(new Font("Arial", Font.BOLD, 16)); // Increased font size
        wordList.setForeground(Color.WHITE); // Set text color to white
        wordList.setBackground(new Color(0, 0, 0, 0)); // Make background transparent
        sidePanel.add(new JLabel("Word List") {
            {
                setForeground(Color.WHITE); // Set label text color to white
                setFont(new Font("Arial", Font.BOLD, 16)); // Increased font size
            }
        });
        sidePanel.add(new JScrollPane(wordList));

        // Score label
        scoreLabel = new JLabel("Score: " + score);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Increased font size
        scoreLabel.setForeground(Color.WHITE); // Set text color to white
        sidePanel.add(scoreLabel);

        // Add Submit and Reset Buttons
        JButton submitButton = new JButton("Answer Question");
        submitButton.addActionListener(e -> askQuestion());
        sidePanel.add(submitButton);

        JButton resetButton = new JButton("Reset Puzzle");
        resetButton.addActionListener(e -> resetPuzzle());
        sidePanel.add(resetButton);

        add(sidePanel, BorderLayout.EAST);

        setSize(600, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        updateWordList();
    }

    private void askQuestion() {
        if (questionIndex < questionKeys.length) {
            String question = questionKeys[questionIndex];
            String answer = questions.get(question);

            // Ask the question
            String userAnswer = JOptionPane.showInputDialog(this, question, "Question", JOptionPane.QUESTION_MESSAGE);

            if (userAnswer != null && userAnswer.equalsIgnoreCase(answer)) {
                if (!trie.search(answer)) {
                    trie.insert(answer); // Insert the user's word into the Trie if it isn't already present
                }

                // Automatically place the word on the grid
                if (crossword.placeWordAutomatically(answer)) {
                    score += answer.length();
                    updateGrid();
                    updateScore();
                    wordList.append("\n" + answer);
                } else {
                    JOptionPane.showMessageDialog(this, "Cannot place word on the grid.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Incorrect answer.");
            }

            questionIndex++;
        } else {
            JOptionPane.showMessageDialog(this, "No more questions.");
        }
    }

    private void updateGrid() {
        for (int i = 0; i < gridFields.length; i++) {
            for (int j = 0; j < gridFields[i].length; j++) {
                gridFields[i][j].setText(Character.toString(crossword.grid[i][j]));
            }
        }
    }

    private void resetPuzzle() {
        crossword.resetGrid();
        updateGrid();
        score = 0;
        updateScore();
        wordList.setText("Word List");
        questionIndex = 0; // Reset the question index
    }

    private void updateScore() {
        scoreLabel.setText("Score: " + score);
    }

    private void updateWordList() {
        wordList.setText("Enter your words and place them on the grid.");
    }
}

// Main class to run the crossword puzzle game
public class CrosswordPuzzleGame {
    public static void main(String[] args) {
        // Initialize an empty Trie
        Trie trie = new Trie();

        // Load the background image and initialize the Crossword in the UI thread
        SwingUtilities.invokeLater(() -> {
            Image backgroundImage = null;
            try {
                backgroundImage = ImageIO.read(new File("C:\\Users\\SUBIKSHAN D\\OneDrive\\Desktop\\Java\\background.jpeg"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Sample questions and answers
            Map<String, String> questions = new HashMap<>();
            questions.put("Capital of France", "Paris");
            questions.put("Largest planet in our solar system", "Jupiter");
            questions.put("Fastest land animal", "Cheetah");
            questions.put("Hardest natural substance", "Diamond");
            questions.put("Lightest element", "Hydrogen");
            questions.put("Highest mountain in the world", "Everest");
            questions.put("Longest river in the world", "Nile");
            questions.put("Smallest country by area", "Vatican");
            questions.put("Author of 'Hamlet'", "Shakespeare");
            questions.put("First element on the periodic table", "Hydrogen");
            questions.put("The currency of Japan", "Yen");
            questions.put("Largest ocean on Earth", "Pacific");
            questions.put("The red planet", "Mars");
            questions.put("The main gas in Earth's atmosphere", "Nitrogen");
            questions.put("The inventor of the light bulb", "Edison");
            questions.put("The largest mammal", "Blue Whale");
            questions.put("The capital city of Italy", "Rome");
            questions.put("The hottest planet in the solar system", "Venus");
            questions.put("The inventor of the telephone", "Bell");
            questions.put("The largest desert in the world", "Sahara");
            questions.put("The most spoken language in the world", "Mandarin");
            questions.put("The currency of the USA", "Dollar");
            questions.put("The author of 'Harry Potter'", "Rowling");
            questions.put("The tallest building in the world", "Burj Khalifa");
            questions.put("The chemical symbol for water", "H2O");
            questions.put("The largest continent", "Asia");

            // Initialize the Crossword with grid size and Trie for validation
            int rows = 10, cols = 10;
            Crossword crossword = new Crossword(rows, cols, trie);

            // Initialize and display the UI
            new CrosswordUI(rows, cols, trie, crossword, backgroundImage, questions);
        });
    }
}