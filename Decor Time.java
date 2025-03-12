
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.time.LocalDate;
import javax.swing.text.MutableAttributeSet;
import java.io.Serializable;
import java.io.*;






public class DecorTime implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient JFrame mainFrame; // 직렬화 제외
    private transient Component currentPanel;
    private HashMap<String, User> userDatabase = new HashMap<>();
    private int currentYear = LocalDate.now().getYear();
    private int currentStartMonth = LocalDate.now().getMonthValue();
    private String currentUserId;
    private HashMap<String, Color> dateColors = new HashMap<>();
    private List<DiaryEntry> diaryDatabase = new ArrayList<>();
    private List<String> stickerTitles = new ArrayList<>();
    private transient JTextPane contentArea;
    private transient BasePanel diaryEditorPanel;
    private HashMap<String, HashMap<String, List<String>>> userScheduleDatabase = new HashMap<>();
    private HashMap<String, List<DiaryEntry>> userDiaryDatabase = new HashMap<>();
    private List<String> stickerDatabase = new ArrayList<>(); // 파일 경로를 저장
    private boolean isFrameInitialized = false; // 초기화 여부 플래그
    
    




    private void save(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(userDatabase); // 사용자 데이터 저장
            oos.writeObject(userScheduleDatabase); // 사용자별 스케줄 저장
            oos.writeObject(userDiaryDatabase); // 사용자별 일기 저장
            oos.writeObject(dateColors); // 날짜 색상 정보 저장
            oos.writeObject(stickerDatabase); // 스티커 정보 저장
            oos.writeObject(stickerTitles); // 스티커 제목 저장
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "데이터 저장 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }





    public static DecorTime load(String filePath) {
        DecorTime loadedInstance = null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            loadedInstance = new DecorTime();
            loadedInstance.userDatabase = (HashMap<String, User>) ois.readObject();
            loadedInstance.userScheduleDatabase = (HashMap<String, HashMap<String, List<String>>>) ois.readObject();
            loadedInstance.userDiaryDatabase = (HashMap<String, List<DiaryEntry>>) ois.readObject();
            loadedInstance.dateColors = (HashMap<String, Color>) ois.readObject();
            loadedInstance.stickerDatabase = (List<String>) ois.readObject();
            loadedInstance.stickerTitles = (List<String>) ois.readObject();

            // 사용자 목록 출력
            loadedInstance.printUserDatabase();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "데이터 로드 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
        return loadedInstance;
    }




    
    private void saveUserData() {
        if (currentUserId == null) return; // 로그인한 사용자가 없으면 저장하지 않음

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("user_" + currentUserId + ".ser"))) {
            // 현재 사용자 데이터 저장
            oos.writeObject(userScheduleDatabase.getOrDefault(currentUserId, new HashMap<>())); // 스케줄 데이터
            oos.writeObject(userDiaryDatabase.getOrDefault(currentUserId, new ArrayList<>())); // 일기 데이터
            oos.writeObject(dateColors); // 날짜 색상 데이터
            oos.writeObject(stickerDatabase); // 스티커 경로 데이터
            oos.writeObject(stickerTitles); // 스티커 제목 데이터
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "데이터 저장 중 오류가 발생했습니다.", "저장 실패", JOptionPane.ERROR_MESSAGE);
        }
    }




    
    private void loadUserData() {
        if (currentUserId == null) return;

        File userDataFile = new File("user_" + currentUserId + ".ser");
        if (userDataFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(userDataFile))) {
                userScheduleDatabase.put(currentUserId, (HashMap<String, List<String>>) ois.readObject());
                userDiaryDatabase.put(currentUserId, (List<DiaryEntry>) ois.readObject());
                dateColors = (HashMap<String, Color>) ois.readObject();
                stickerDatabase = (List<String>) ois.readObject();
                stickerTitles = (List<String>) ois.readObject();

                // `diaryDatabase`를 항상 로드된 데이터로 초기화
                diaryDatabase = new ArrayList<>(userDiaryDatabase.getOrDefault(currentUserId, new ArrayList<>()));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame, "데이터 로드 중 오류가 발생했습니다.", "로드 실패", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // 데이터 초기화
            userScheduleDatabase.put(currentUserId, new HashMap<>());
            userDiaryDatabase.put(currentUserId, new ArrayList<>());
            diaryDatabase = new ArrayList<>();
            dateColors = new HashMap<>();
            stickerDatabase = new ArrayList<>();
            stickerTitles = new ArrayList<>();
        }
    }


    private void printUserDatabase() {
        System.out.println("==== 현재 회원가입된 사용자 목록 ====");
        if (userDatabase.isEmpty()) {
            System.out.println("등록된 사용자가 없습니다.");
        } else {
            for (User user : userDatabase.values()) {
                System.out.println("이름: " + user.getName() + ", 아이디: " + user.getId() + ", 비밀번호: " + user.getPassword());
            }
        }
        System.out.println("===================================");
    }






    
    public DecorTime() {
        initializeMainFrame();
        createLoginUI();
    }

    private void initializeMainFrame() {
        if (isFrameInitialized) return; // 이미 초기화된 경우 실행하지 않음
        mainFrame = new JFrame("DecorTime");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH); // 화면 전체 크기
        mainFrame.setLayout(null);
        mainFrame.getContentPane().setBackground(Color.BLACK); // 검은색 배경
        mainFrame.setVisible(true); // 프레임 표시
        isFrameInitialized = true; // 초기화 완료
    }


    private void switchPanel(Component newPanel) {
        if (currentPanel == newPanel) return; // 같은 패널로 전환하지 않음
        if (currentPanel != null) {
            mainFrame.getContentPane().remove(currentPanel);
        }
        currentPanel = newPanel;
        mainFrame.add(newPanel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }


    
    class BasePanel extends JPanel {
        public BasePanel(Color backgroundColor) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int panelWidth = 400; // 고정 너비
            int panelHeight = (int) (screenSize.height * 0.85); // 화면 높이의 85% 사용
            int panelX = (screenSize.width - panelWidth) / 2; // 중앙 정렬
            int panelY = (screenSize.height - panelHeight) / 3;

            setBounds(panelX, panelY, panelWidth, panelHeight);
            setBackground(backgroundColor);
            setLayout(null);
        }
    }
    
    
    private void createLoginUI() {
    	BasePanel loginPanel = new BasePanel(Color.WHITE);
        
        JLabel titleLabel = new JLabel("<html><span style='color:purple;'>Decor</span><span style='color:lavender;'>Time</span></html>", SwingConstants.CENTER);
        titleLabel.setBounds(0, 100, loginPanel.getWidth(), 100);
        titleLabel.setFont(titleLabel.getFont().deriveFont(45f));
        loginPanel.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("세상에 하나밖에 없는 나만의 캘린더", SwingConstants.CENTER);
        subtitleLabel.setBounds(0, 170, loginPanel.getWidth(), 30);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(12f));
        subtitleLabel.setForeground(Color.LIGHT_GRAY);
        loginPanel.add(subtitleLabel);


        RoundedTextField idField = new RoundedTextField("아이디");
        idField.setBounds(50, 448, 300, 40);
        loginPanel.add(idField);

        RoundedPasswordField passwordField = new RoundedPasswordField("비밀번호");
        passwordField.setBounds(50, 500, 300, 40);
        loginPanel.add(passwordField);

        RoundedButton loginButton = new RoundedButton("로그인");
        loginButton.setBounds(50, 570, 300, 40);
        loginButton.setBackground(new Color(200, 162, 200)); 
        loginButton.setForeground(Color.WHITE);
        loginPanel.add(loginButton);

        loginButton.addActionListener(e -> {
            String id = idField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (userDatabase.containsKey(id) && userDatabase.get(id).getPassword().equals(password)) {
                currentUserId = id;

                // 사용자 데이터 로드
                loadUserData();

                JOptionPane.showMessageDialog(mainFrame, "로그인 성공!", "성공", JOptionPane.INFORMATION_MESSAGE);
                createCalendarUI(); // 캘린더 UI 생성
            } else {
                JOptionPane.showMessageDialog(mainFrame, "아이디 또는 비밀번호가 틀렸습니다.", "로그인 실패", JOptionPane.ERROR_MESSAGE);
            }
        });





        JLabel signUpLabel = new JLabel("회원가입", SwingConstants.RIGHT);
        signUpLabel.setForeground(Color.GRAY);
        signUpLabel.setBounds(270, 620, 80, 20);
        signUpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginPanel.add(signUpLabel);

        signUpLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                createSignUpUI();
            }
        });

        switchPanel(loginPanel);
    }

    
    class RoundedButton extends JButton {
        public RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
        }


        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }

        protected void paintBorder(Graphics g) {
            // 테두리를 그리지 않음
        }
    }
    
    


    // 둥근 텍스트 필드
    class RoundedTextField extends JTextField {
        private final String placeholder;
        public RoundedTextField(String placeholder) {
            this.placeholder = placeholder;
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            setForeground(Color.GRAY);
            setText(placeholder);
            setOpaque(false);

            // 포커스 리스너 추가
            addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (getText().equals(placeholder)) {
                        setText("");
                        setForeground(Color.BLACK);
                    }
                }

                public void focusLost(java.awt.event.FocusEvent e) {
                    if (getText().isEmpty()) {
                        setForeground(Color.GRAY);
                        setText(placeholder);
                    }
                }
            });
        }


        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            super.paintComponent(g);
            g2.dispose();
        }
    }

    // 둥근 비밀번호 필드
    class RoundedPasswordField extends JPasswordField {
        private final String placeholder;
        private boolean showingPlaceholder = true;

        public RoundedPasswordField(String placeholder) {
            this.placeholder = placeholder;
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            setForeground(Color.GRAY);
            setText(placeholder);
            setOpaque(false);
            setEchoChar((char) 0); // 기본적으로 보호 해제

            // 포커스 리스너
            addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (showingPlaceholder) {
                        setText("");
                        setForeground(Color.gray);
                        setEchoChar('●'); // 입력 시 보호 활성화
                        showingPlaceholder = false;
                    }
                }
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (new String(getPassword()).isEmpty()) {
                        setForeground(Color.GRAY);
                        setText(placeholder);
                        setEchoChar((char) 0); // 보호 비활성화
                        showingPlaceholder = true;
                    }
                }
            });

            // 키 입력 리스너
            addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (showingPlaceholder) {
                        setText(""); // 기본 텍스트 제거
                        setForeground(Color.gray);
                        setEchoChar('●'); // 보호 활성화
                        showingPlaceholder = false;
                    }
                }
            });
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            super.paintComponent(g);
            g2.dispose();
        }
    }





    private void createSignUpUI() {
    	 BasePanel signUpPanel = new BasePanel(Color.WHITE);

        JPanel bannerPanel = new JPanel();
        bannerPanel.setBounds(0, 15, signUpPanel.getWidth(), 50);
        bannerPanel.setBackground(new Color(200, 160, 205));
        bannerPanel.setLayout(null); // 배치 관리자 해제

        // "←" 버튼 추가
        JLabel backLabel = new JLabel("←", SwingConstants.LEFT);
        backLabel.setFont(backLabel.getFont().deriveFont(30f)); // 크기 설정
        backLabel.setForeground(Color.WHITE);
        backLabel.setBounds(10, 5, 50, 40); // 위치 및 크기
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bannerPanel.add(backLabel);

        backLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                createLoginUI(); // 로그인 창으로 이동
            }
        });

        // 제목 추가
        JLabel bannerLabel = new JLabel("Sign Up", SwingConstants.CENTER);
        bannerLabel.setForeground(Color.WHITE);
        bannerLabel.setFont(bannerLabel.getFont().deriveFont(20f));
        bannerLabel.setBounds(60, 5, signUpPanel.getWidth() -100, 40); // "←" 옆에 위치
        bannerPanel.add(bannerLabel);

        signUpPanel.add(bannerPanel);

        String defaultImagePath = "src/image/image1.png";
        File defaultImageFile = new File(defaultImagePath);
        if (!defaultImageFile.exists()) {
            JOptionPane.showMessageDialog(mainFrame, "기본 이미지가 존재하지 않습니다. 기본 이미지 경로를 확인하세요.");
        }

        JLabel imageLabel = new CircleImageLabel(defaultImagePath);
        imageLabel.setBounds(50, 100, 280, 280);
        signUpPanel.add(imageLabel);

        imageLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(mainFrame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    ((CircleImageLabel) imageLabel).setImagePath(selectedFile.getAbsolutePath());
                    imageLabel.repaint();
                }
            }
        });

        // 이름 입력 필드
        RoundedTextField nameField = new RoundedTextField("이름");
        nameField.setBounds(50, 420, 300, 40);
        signUpPanel.add(nameField);

        // 아이디 입력 필드
        RoundedTextField idField = new RoundedTextField("아이디");
        idField.setBounds(50, 472, 300, 40);
        signUpPanel.add(idField);

        // 비밀번호 입력 필드
        RoundedPasswordField passwordField = new RoundedPasswordField("비밀번호");
        passwordField.setBounds(50, 523, 300, 40);
        signUpPanel.add(passwordField);

        // 생성 버튼
        RoundedButton createButton = new RoundedButton("생성");
        createButton.setBounds(50, 590, 300, 40);
        createButton.setBackground(new Color(200, 162, 200));
        createButton.setForeground(Color.WHITE);
        signUpPanel.add(createButton);

        createButton.addActionListener(e -> {
            String id = idField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String name = nameField.getText().trim();
            String imagePath = ((CircleImageLabel) imageLabel).getImagePath();

            if (name.isEmpty() || name.equals("이름")) {
                JOptionPane.showMessageDialog(mainFrame, "이름을 입력해주세요.", "회원가입 실패", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (id.isEmpty() || id.equals("아이디")) {
                JOptionPane.showMessageDialog(mainFrame, "아이디를 입력해주세요.", "회원가입 실패", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (password.isEmpty() || password.equals("비밀번호")) {
                JOptionPane.showMessageDialog(mainFrame, "비밀번호를 입력해주세요.", "회원가입 실패", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (userDatabase.containsKey(id)) {
                JOptionPane.showMessageDialog(mainFrame, "이미 존재하는 아이디입니다.", "회원가입 실패", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 사용자 데이터 생성 및 저장
            userDatabase.put(id, new User(id, password, name, imagePath));
            userScheduleDatabase.put(id, new HashMap<>());
            userDiaryDatabase.put(id, new ArrayList<>());

            // 전체 데이터 저장
            save("user_data.ser");

            JOptionPane.showMessageDialog(mainFrame, "회원가입이 완료되었습니다!", "성공", JOptionPane.INFORMATION_MESSAGE);
            createLoginUI();
        });



        switchPanel(signUpPanel);
    }



    private void createCalendarUI() {
        if (currentUserId == null || !userDatabase.containsKey(currentUserId)) {
            JOptionPane.showMessageDialog(mainFrame, "사용자 정보가 없습니다. 로그인 후 다시 시도해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
            createLoginUI();
            return;
        }

        // 현재 사용자의 스케줄 데이터 가져오기
        HashMap<String, List<String>> scheduleData = userScheduleDatabase.get(currentUserId);
        AtomicReference<String> currentSelectedDate = new AtomicReference<>(getFormattedDate(LocalDate.now()));
        AtomicReference<RoundedButton> selectedDateButtonRef = new AtomicReference<>(null);
        BasePanel calendarPanel = new BasePanel(Color.WHITE);

        CircleImageLabel userImageLabel = new CircleImageLabel(userDatabase.get(currentUserId).getImagePath());
        userImageLabel.setBounds(calendarPanel.getWidth() - 60, 10, 40, 40);
        calendarPanel.add(userImageLabel);

        userImageLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                createSettingsUI();
            }
        });

        
        
     // 캘린더 UI 상단에 추가
        JLabel diaryLabel = new JLabel("📓", SwingConstants.CENTER);
        diaryLabel.setBounds(userImageLabel.getX() - 50, 10, 40, 30); // "설정" 버튼 옆
        diaryLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        diaryLabel.setFont(diaryLabel.getFont().deriveFont(18f));
        calendarPanel.add(diaryLabel);

        diaryLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                createDiaryListUI();
            }
        });


        int navigationY = 50;

        JLabel leftArrowLabel = new JLabel("◀", SwingConstants.LEFT);
        leftArrowLabel.setBounds(30, navigationY, 30, 30);
        leftArrowLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        calendarPanel.add(leftArrowLabel);

        JLabel monthLabel = new JLabel(
                String.format("<html><span style='font-size:30px; font-weight:bold;'>%d</span> <span style='font-size:10px;'>/%d</span></html>",
                        currentStartMonth, currentYear), SwingConstants.CENTER);
        monthLabel.setBounds((calendarPanel.getWidth() - 200) / 2, navigationY, 200, 40);
        calendarPanel.add(monthLabel);

        JLabel rightArrowLabel = new JLabel("▶", SwingConstants.RIGHT);
        rightArrowLabel.setBounds(calendarPanel.getWidth() - 60, navigationY, 30, 30);
        rightArrowLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        calendarPanel.add(rightArrowLabel);

        leftArrowLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentStartMonth > 1) {
                    currentStartMonth--;
                } else {
                    currentStartMonth = 12;
                    currentYear--;
                }
                createCalendarUI();
            }
        });

        rightArrowLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (currentStartMonth < 12) {
                    currentStartMonth++;
                } else {
                    currentStartMonth = 1;
                    currentYear++;
                }
                createCalendarUI();
            }
        });

        // 캘린더 영역
        RoundedPanel calendarGrid = new RoundedPanel(20, 20);
        calendarGrid.setBounds(20, 100, calendarPanel.getWidth() - 40, calendarPanel.getHeight() / 2 - 100);
        calendarGrid.setLayout(new GridLayout(6, 7, 5, 5));
        calendarPanel.add(calendarGrid);

        JPanel schedulePanel = new JPanel(null);
        schedulePanel.setBounds(20, calendarPanel.getHeight() / 2, calendarPanel.getWidth() - 40, calendarPanel.getHeight() / 2 - 20);
        schedulePanel.setOpaque(false);
        calendarPanel.add(schedulePanel);

        JLabel scheduleTitle = new JLabel(currentSelectedDate.get(), SwingConstants.LEFT);
        scheduleTitle.setBounds(10, 10, 200, 30);
        schedulePanel.add(scheduleTitle);

        // 입력창 및 추가 버튼
        RoundedTextField newScheduleField = new RoundedTextField("스케줄 입력");
        newScheduleField.setBounds(10, 50, schedulePanel.getWidth() - 80, 40);
        schedulePanel.add(newScheduleField);

        RoundedButton addScheduleButton = new RoundedButton("+");
        addScheduleButton.setBounds(schedulePanel.getWidth() - 60, 50, 50, 40);
        addScheduleButton.setBackground(new Color(128, 0, 128)); // Purple
        schedulePanel.add(addScheduleButton);

        // 스케줄 목록
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> scheduleList = new JList<>(listModel);
        JScrollPane scheduleScroll = new JScrollPane(scheduleList);
        scheduleScroll.setBounds(10, 110, schedulePanel.getWidth() - 20, schedulePanel.getHeight() - 120);
        schedulePanel.add(scheduleScroll);

        // 스케줄 저장 및 로드 시 현재 사용자 데이터 사용
        addScheduleButton.addActionListener(e -> {
            String schedule = newScheduleField.getText().trim();
            if (!schedule.isEmpty() && !schedule.equals("스케줄 입력")) {
                userScheduleDatabase.get(currentUserId).computeIfAbsent(currentSelectedDate.get(), k -> new ArrayList<>()).add(schedule);
                loadSchedule(userScheduleDatabase.get(currentUserId), currentSelectedDate.get(), listModel);
                newScheduleField.setText("");

                saveUserData(); // 사용자 데이터 저장
            } else {
                JOptionPane.showMessageDialog(mainFrame, "유효한 스케줄을 입력하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            }
        });



     // 스케줄 삭제 기능 수정 (현재 사용자 데이터 사용)
        scheduleList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) { // 오른쪽 클릭
                    int index = scheduleList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String selectedSchedule = listModel.getElementAt(index); // 선택된 스케줄 가져오기
                        int confirm = JOptionPane.showConfirmDialog(mainFrame, "삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            String currentDate = currentSelectedDate.get();
                            
                            // 현재 사용자 스케줄 데이터에서 삭제
                            HashMap<String, List<String>> userSchedules = userScheduleDatabase.getOrDefault(currentUserId, new HashMap<>());
                            List<String> schedulesForDate = userSchedules.getOrDefault(currentDate, new ArrayList<>());
                            
                            if (schedulesForDate.contains(selectedSchedule)) {
                                schedulesForDate.remove(selectedSchedule); // 스케줄 삭제

                                // 날짜에 스케줄이 없으면 날짜 자체를 제거
                                if (schedulesForDate.isEmpty()) {
                                    userSchedules.remove(currentDate);
                                } else {
                                    userSchedules.put(currentDate, schedulesForDate); // 갱신된 스케줄 목록 저장
                                }

                                userScheduleDatabase.put(currentUserId, userSchedules); // 업데이트된 데이터 저장

                                // 변경된 데이터 저장
                                saveUserData();

                                // UI 업데이트
                                loadSchedule(userSchedules, currentDate, listModel);
                            }
                        }
                    }
                }
            }
        });



        int colorButtonSize = 30; // 버튼 크기
        int gap = 5; // 버튼 간 간격
        Color[] buttonColors = {
            new Color(249, 198, 194), // 연한 빨강
            new Color(189, 214, 144), // 연한 초록
            new Color(143, 179, 229), // 연한 파랑
            new Color(246, 218, 118)  // 연한 분홍
        };

        // 오른쪽 정렬을 위해 초기 X 좌표 계산
        int startX = schedulePanel.getWidth() - ((colorButtonSize + gap) * buttonColors.length) - 10;

        for (int i = 0; i < buttonColors.length; i++) {
            RoundedColorButton colorButton = new RoundedColorButton(buttonColors[i]);
            colorButton.setBounds(startX + (i * (colorButtonSize + gap)), 10, colorButtonSize, colorButtonSize);
            schedulePanel.add(colorButton); // 색상 버튼 패널에 추가

            final Color chosenColor = buttonColors[i];
            colorButton.addActionListener(e -> {
                if (selectedDateButtonRef.get() != null) {
                    String selectedDate = currentSelectedDate.get();
                    if (selectedDate != null) {
                        // 날짜 색상이 이미 설정된 경우 색상 초기화
                        if (dateColors.containsKey(selectedDate) && dateColors.get(selectedDate).equals(chosenColor)) {
                            if (selectedDate.equals(getFormattedDate(LocalDate.now()))) {
                                selectedDateButtonRef.get().setBackground(new Color(180, 180, 255)); // 오늘 강조색
                            } else {
                                selectedDateButtonRef.get().setBackground(new Color(230, 210, 255)); // 기본 색상
                                dateColors.remove(selectedDate); // 색상 초기화
                            }
                        } else {
                            // 새 색상 설정
                            selectedDateButtonRef.get().setBackground(chosenColor);
                            dateColors.put(selectedDate, chosenColor);
                        }

                        saveUserData(); // 변경 즉시 저장
                    }
                }
            });


        }


        // 캘린더 생성
        drawMonth(calendarGrid, currentYear, currentStartMonth, scheduleData, currentSelectedDate, scheduleTitle, listModel, selectedDateButtonRef, dateColors);

        // 패널 전환
        switchPanel(calendarPanel);
    }

    private void drawMonth(
            RoundedPanel calendarGrid,
            int year,
            int month,
            HashMap<String, List<String>> scheduleData,
            AtomicReference<String> currentSelectedDate,
            JLabel scheduleTitle,
            DefaultListModel<String> listModel,
            AtomicReference<RoundedButton> selectedDateButtonRef,
            HashMap<String, Color> dateColors) {

        calendarGrid.removeAll(); // 기존 달력 내용 제거

        // 요일 헤더 추가
        String[] days = {"일", "월", "화", "수", "목", "금", "토"};
        for (String day : days) {
            JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
            dayLabel.setHorizontalAlignment(SwingConstants.CENTER);
            calendarGrid.add(dayLabel);
        }

        // 날짜 계산
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 이번 달의 첫 요일
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH); // 이번 달의 총 일수
        LocalDate today = LocalDate.now(); // 오늘 날짜
        String todayKey = today.getYear() + "년 " + today.getMonthValue() + "월 " + today.getDayOfMonth() + "일"; // 오늘 날짜 키

        // 캘린더의 행과 열을 7열 × 5행으로 고정
        int totalCells = 7 * 5; // 7열 × 5행 = 35개 셀
        int dayCounter = 1;

        for (int i = 0; i < totalCells; i++) {
            if (i < firstDayOfWeek - 1 || dayCounter > daysInMonth) {
                // 빈 셀 추가
                calendarGrid.add(new JLabel());
            } else {
                // 날짜 버튼 생성
                String dateKey = year + "년 " + month + "월 " + dayCounter + "일";
                RoundedButton dateButton = new RoundedButton(String.valueOf(dayCounter));
                
                
                
                // 오늘 날짜 강조
                if (dateKey.equals(todayKey)) {
                    dateButton.setBackground(new Color(180, 180, 255)); // 오늘 날짜 강조 색상
                } else {
                    dateButton.setBackground(dateColors.getOrDefault(dateKey, new Color(230, 210, 255))); // 저장된 색상 또는 기본 색상
                }

                // 날짜 클릭 이벤트 처리
                dateButton.addActionListener(e -> {
                    // 이전 선택된 버튼 복원
                    if (selectedDateButtonRef.get() != null) {
                        RoundedButton prevButton = selectedDateButtonRef.get();
                        String prevDateKey = currentSelectedDate.get();
                        if (dateColors.containsKey(prevDateKey)) {
                            prevButton.setBackground(dateColors.get(prevDateKey)); // 저장된 색상 복원
                        } else if (prevDateKey.equals(todayKey)) {
                            prevButton.setBackground(new Color(180, 180, 255)); // 오늘 날짜 복원
                        } else {
                            prevButton.setBackground(new Color(230, 210, 255)); // 기본 색상 복원
                        }
                    }

                    // 현재 버튼 강조
                    dateButton.setBackground(new Color(200, 200, 255)); // 선택 강조 색상
                    selectedDateButtonRef.set(dateButton);

                    // 선택된 날짜 정보 업데이트
                    currentSelectedDate.set(dateKey);
                    scheduleTitle.setText(dateKey);

                    // 스케줄 로드
                    loadSchedule(scheduleData, dateKey, listModel);
                });

                
                calendarGrid.add(dateButton); // 버튼 추가
                dayCounter++;
            }
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }



    //스케줄 로드
    private void loadSchedule(HashMap<String, java.util.List<String>> scheduleData, String selectedDate, DefaultListModel<String> listModel) {
    	listModel.clear();
    	if (scheduleData.containsKey(selectedDate)) {
    		for (String schedule : scheduleData.get(selectedDate)) {
    			listModel.addElement(schedule);
    		}
    	}
    }
    
    //스케줄 UI 생성
    private JPanel createScheduleUI(AtomicReference<String> currentSelectedDate,
                     HashMap<String, java.util.List<String>> scheduleData,
                     DefaultListModel<String> listModel) {
    	JPanel schedulePanel = new JPanel(null);
    	schedulePanel.setBounds(20, 400, 760, 300);

    	// 상단 날짜
    	JLabel scheduleTitle = new JLabel(currentSelectedDate.get(), SwingConstants.LEFT);
    	scheduleTitle.setBounds(10, 10, 300, 30);
    	schedulePanel.add(scheduleTitle);

    	// 스케줄 목록
    	JList<String> scheduleList = new JList<>(listModel);
    	JScrollPane scheduleScroll = new JScrollPane(scheduleList);
    	scheduleScroll.setBounds(10, 50, schedulePanel.getWidth() - 20, 180);
    	schedulePanel.add(scheduleScroll);

    	// 스케줄 입력창
    	RoundedTextField scheduleInput = new RoundedTextField("스케줄 입력");
    	scheduleInput.setBounds(10, 240, 600, 40);
    	schedulePanel.add(scheduleInput);

    	// 추가 버튼
    	RoundedButton addButton = new RoundedButton("추가");
    	addButton.setBounds(620, 240, 120, 40);
    	addButton.setBackground(new Color(80, 150, 80));
    	addButton.setForeground(Color.WHITE);
    	schedulePanel.add(addButton);

    	addButton.addActionListener(e -> {
    		String newSchedule = scheduleInput.getText().trim();
    		if (!newSchedule.isEmpty() && !newSchedule.equals("스케줄 입력")) {
    			scheduleData.computeIfAbsent(currentSelectedDate.get(), k -> new ArrayList<>()).add(newSchedule);
    			listModel.addElement(newSchedule);
    			scheduleInput.setText("");
    		} else {
    			JOptionPane.showMessageDialog(mainFrame, "스케줄을 입력하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
    		}
    	});

    	// 삭제 버튼
    	RoundedButton deleteButton = new RoundedButton("삭제");
    	deleteButton.setBounds(10, 290, 120, 40);
    	deleteButton.setBackground(new Color(150, 80, 80));
    	deleteButton.setForeground(Color.WHITE);
    	schedulePanel.add(deleteButton);

    	deleteButton.addActionListener(e -> {
    		int selectedIndex = scheduleList.getSelectedIndex();
    		if (selectedIndex != -1) {
    			String removedSchedule = listModel.get(selectedIndex);
    			listModel.remove(selectedIndex);
    			scheduleData.getOrDefault(currentSelectedDate.get(), new ArrayList<>()).remove(removedSchedule);
    		} else {
    			JOptionPane.showMessageDialog(mainFrame, "삭제할 스케줄을 선택하세요.", "삭제 오류", JOptionPane.WARNING_MESSAGE);
    		}
    	});

    	return schedulePanel;
    }



    private String getFormattedDate(LocalDate date) {
        return date.getYear() + "년 " + date.getMonthValue() + "월 " + date.getDayOfMonth() + "일";
    }

    
    class RoundedPanel extends JPanel {
        private int arcWidth;
        private int arcHeight;
        

        public RoundedPanel(int arcWidth, int arcHeight) {
            this.arcWidth = arcWidth;
            this.arcHeight = arcHeight;
            setOpaque(false); // 배경을 투명하게 설정
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.dispose();
        }
    }

    
    
    private void createDiaryListUI() {
        BasePanel diaryListPanel = new BasePanel(Color.WHITE);
        List<DiaryEntry> diaryDatabase = userDiaryDatabase.getOrDefault(currentUserId, new ArrayList<>());

        JPanel topPanel = new JPanel(null);
        topPanel.setBounds(0, 0, diaryListPanel.getWidth(), 80);
        topPanel.setBackground(Color.WHITE);
        diaryListPanel.add(topPanel);

        JLabel backLabel = new JLabel("←", SwingConstants.LEFT);
        backLabel.setBounds(10, 20, 50, 40);
        backLabel.setFont(backLabel.getFont().deriveFont(30f));
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        topPanel.add(backLabel);

        backLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                createCalendarUI();
            }
        });

        JLabel titleLabel = new JLabel(userDatabase.get(currentUserId).getName() + "님의 일기장", SwingConstants.CENTER);
        titleLabel.setBounds(60, 20, diaryListPanel.getWidth() - 120, 40);
        topPanel.add(titleLabel);

        JLabel addDiaryLabel = new JLabel("⨁", SwingConstants.RIGHT);
        addDiaryLabel.setBounds(diaryListPanel.getWidth() - 80, 20, 40, 40);
        addDiaryLabel.setFont(addDiaryLabel.getFont().deriveFont(30f));
        addDiaryLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        topPanel.add(addDiaryLabel);

        addDiaryLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                createDiaryEditorUI(null); // 새 일기 작성 UI로 이동
            }
        });

        // 구분선 추가
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setBounds(20, 80, diaryListPanel.getWidth() - 40, 1);
        diaryListPanel.add(separator);

        JPanel middlePanel = new JPanel();
        middlePanel.setBounds(20, 100, diaryListPanel.getWidth() - 40, diaryListPanel.getHeight() - 170);
        middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.Y_AXIS));
        middlePanel.setBackground(Color.WHITE);
        diaryListPanel.add(middlePanel);

        JPanel paginationPanel = new JPanel(new FlowLayout());
        paginationPanel.setBounds(10, diaryListPanel.getHeight() - 50, diaryListPanel.getWidth() - 20, 40);
        paginationPanel.setBackground(Color.WHITE);
        diaryListPanel.add(paginationPanel);

        // 중간 패널 초기화 및 데이터 로드
        middlePanel.removeAll(); // 기존 내용 초기화
        paginationPanel.removeAll(); // 페이지네이션 패널 초기화

        if (diaryDatabase.isEmpty()) {
            // 일기가 없을 때 문구 추가
            JLabel quoteLabel = new JLabel("<html><div style='text-align:center;'>"
                    + "동트기전에 일어나라. 기록하기를 좋아하라.<br>"
                    + "쉬지말고 기록하라. 생각이 떠오르면 수시로 기록하라.<br>"
                    + "기억은 흐려지고 생각은 사라진다.<br>"
                    + "머리를 믿지말고 손을 믿어라."
                    + "</div></html>", SwingConstants.CENTER);
            quoteLabel.setForeground(Color.GRAY);
            quoteLabel.setFont(quoteLabel.getFont().deriveFont(12f));
            quoteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel authorLabel = new JLabel("<다산 정약용>", SwingConstants.RIGHT);
            authorLabel.setForeground(Color.GRAY);
            authorLabel.setFont(authorLabel.getFont().deriveFont(10f));
            authorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            middlePanel.add(Box.createVerticalGlue()); // 상단 여백 추가
            middlePanel.add(quoteLabel);
            middlePanel.add(Box.createRigidArea(new Dimension(0, 10))); // 문구와 저자 간 간격 추가
            middlePanel.add(authorLabel);
            middlePanel.add(Box.createVerticalGlue()); // 하단 여백 추가

            // 페이지네이션 패널 숨김 처리
            paginationPanel.setVisible(false);
        } else {
            loadDiaryPage(0, middlePanel);
            updatePagination(paginationPanel, middlePanel, diaryDatabase.size());
            paginationPanel.setVisible(true);
        }

        // 모든 패널 상태 갱신
        middlePanel.revalidate();
        middlePanel.repaint();
        paginationPanel.revalidate();
        paginationPanel.repaint();

        diaryListPanel.revalidate();
        diaryListPanel.repaint();

        switchPanel(diaryListPanel);
    }



    // 페이지네이션 갱신 메서드
    private void updatePagination(JPanel paginationPanel, JPanel middlePanel, int totalItems) {
        paginationPanel.removeAll(); // 기존 버튼 제거

        int totalPages = Math.max(0, (int) Math.ceil(totalItems / 10.0)); 

        for (int i = 0; i < totalPages; i++) {
            RoundedButton pageButton = new RoundedButton(String.valueOf(i + 1));
            pageButton.setBackground(new Color(200, 162, 200)); // 배경색
            int pageIndex = i;
            pageButton.addActionListener(e -> loadDiaryPage(pageIndex, middlePanel));
            paginationPanel.add(pageButton);
        }

        paginationPanel.revalidate();
        paginationPanel.repaint();
    }


    // 일기 삭제 시 동적 업데이트
    private void loadDiaryPage(int page, JPanel middlePanel) {
        middlePanel.removeAll();
        int startIndex = page * 10;
        int endIndex = Math.min(startIndex + 10, diaryDatabase.size());

        for (int i = startIndex; i < endIndex; i++) {
            DiaryEntry diary = diaryDatabase.get(i);
            JPanel diaryPanel = new JPanel(new BorderLayout());
            diaryPanel.setPreferredSize(new Dimension(middlePanel.getWidth(), 40));
            diaryPanel.setBackground(Color.WHITE);

            JLabel titleLabel = new JLabel(diary.getTitle());
            JLabel dateLabel = new JLabel(diary.getDate(), SwingConstants.RIGHT);

            diaryPanel.add(titleLabel, BorderLayout.WEST);
            diaryPanel.add(dateLabel, BorderLayout.EAST);

            diaryPanel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        // 왼쪽 클릭: createDiaryViewerUI로 이동
                        createDiaryViewerUI(diary);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        // 오른쪽 클릭: 삭제 확인 후 삭제
                        int confirm = JOptionPane.showConfirmDialog(
                                mainFrame,
                                diary.getTitle() + "을(를) 삭제하시겠습니까?",
                                "삭제 확인",
                                JOptionPane.YES_NO_OPTION
                        );
                        if (confirm == JOptionPane.YES_OPTION) {
                            // 데이터 삭제
                            diaryDatabase.remove(diary); // 현재 세션 데이터에서 삭제
                            userDiaryDatabase.get(currentUserId).remove(diary); // 사용자 데이터베이스에서 삭제

                            // 변경된 데이터 저장
                            saveUserData(); // 변경사항을 파일에 저장

                            // UI 갱신
                            createDiaryListUI();
                        }
                    }
                }
            });

            middlePanel.add(diaryPanel);

            // 구분선 추가
            JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
            separator.setMaximumSize(new Dimension(middlePanel.getWidth(), 1));
            middlePanel.add(separator);
        }

        middlePanel.revalidate();
        middlePanel.repaint();
    }




    private void createDiaryViewerUI(DiaryEntry diary) {
        BasePanel diaryViewerPanel = new BasePanel(Color.WHITE);
        diaryViewerPanel.setLayout(null);

        // 상단 패널
        JPanel headerPanel = new JPanel(null);
        headerPanel.setBounds(0, 0, diaryViewerPanel.getWidth(), 50);
        headerPanel.setBackground(new Color(200, 162, 200));
        diaryViewerPanel.add(headerPanel);

        JLabel backLabel = new JLabel("←", SwingConstants.LEFT);
        backLabel.setFont(backLabel.getFont().deriveFont(20f));
        backLabel.setBounds(10, 10, 50, 30);
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerPanel.add(backLabel);
        backLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                createDiaryListUI(); // 목록으로 돌아가기
            }
        });

        JLabel titleLabel = new JLabel(diary.getTitle(), SwingConstants.CENTER);
        titleLabel.setBounds((diaryViewerPanel.getWidth() - 200) / 2, 10, 200, 30);
        headerPanel.add(titleLabel);

        RoundedButton editButton = new RoundedButton("수정");
        editButton.setBackground(Color.WHITE); 
        editButton.setForeground(new Color(200, 162, 200));
        editButton.setBounds(diaryViewerPanel.getWidth() - 80, 10, 60, 30);
        headerPanel.add(editButton);
        editButton.addActionListener(e -> createDiaryEditorUI(diary));

        // 본문 영역
        JTextPane contentPane = new JTextPane();
        contentPane.setContentType("text/html");

        // HTML 콘텐츠 로드 및 정리
        String displayHtmlContent = sanitizeHtmlForDisplay(diary.getContent());
        contentPane.setText(displayHtmlContent);
        contentPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(contentPane);
        scrollPane.setBounds(10, 60, diaryViewerPanel.getWidth() - 20, diaryViewerPanel.getHeight() - 70);
        diaryViewerPanel.add(scrollPane);

        switchPanel(diaryViewerPanel);
    }



    


    private void createDiaryEditorUI(DiaryEntry diary) {
        diaryEditorPanel = new BasePanel(Color.WHITE); // 클래스 변수 초기화
        diaryEditorPanel.setLayout(null);

        AtomicReference<Color> currentTextColor = new AtomicReference<>(Color.BLACK); // 기본 텍스트 색상

        // 본문 입력 영역
        contentArea = new JTextPane();
        contentArea.setContentType("text/html");
        
        
        // 상단 헤더
        JPanel headerPanel = new JPanel(null);
        headerPanel.setBounds(0, 0, diaryEditorPanel.getWidth(), 50);
        headerPanel.setBackground(new Color(200, 162, 200));
        diaryEditorPanel.add(headerPanel);

        JLabel backLabel = new JLabel("←", SwingConstants.LEFT);
        backLabel.setFont(backLabel.getFont().deriveFont(25f));
        backLabel.setBounds(10, 10, 50, 30);
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerPanel.add(backLabel);
        backLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                createDiaryListUI(); // 목록으로 돌아가기
            }
        });

        JLabel titleLabel = new JLabel("일기장", SwingConstants.CENTER);
        titleLabel.setBounds((diaryEditorPanel.getWidth() - 100) / 2, 10, 100, 30);
        headerPanel.add(titleLabel);

        // 색상 버튼
        RoundedColorButton colorButton = new RoundedColorButton(currentTextColor.get());
        colorButton.setBounds(10, 60, 30, 30);
        diaryEditorPanel.add(colorButton);

        colorButton.addActionListener(e -> {
            Color selectedColor = JColorChooser.showDialog(mainFrame, "텍스트 색상 선택", currentTextColor.get());
            if (selectedColor != null) {
                currentTextColor.set(selectedColor);
                colorButton.setBackground(selectedColor);

                // 선택된 텍스트에 색상 적용
                StyledDocument doc = contentArea.getStyledDocument();
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, selectedColor);

                int start = contentArea.getSelectionStart();
                int end = contentArea.getSelectionEnd();

                if (start != end) {
                    doc.setCharacterAttributes(start, end - start, attrs, false);
                } else {
                    contentArea.setCharacterAttributes(attrs, false);
                }
            }
        });

        // 한글 조합 상태에서 실시간으로 색상 적용
        contentArea.addInputMethodListener(new InputMethodListener() {
            private boolean compositionActive = false;


            public void inputMethodTextChanged(InputMethodEvent event) {
                if (compositionActive) {
                    StyledDocument doc = contentArea.getStyledDocument();
                    SimpleAttributeSet attrs = new SimpleAttributeSet();
                    StyleConstants.setForeground(attrs, currentTextColor.get());

                    try {
                        int length = doc.getLength();
                        if (length > 0) {
                            // 현재 조합 중인 한글에 색상 적용
                            doc.setCharacterAttributes(length - 1, 1, attrs, false);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }


            public void caretPositionChanged(InputMethodEvent event) {
                compositionActive = true; // 조합 상태 시작
            }
        });

        // DocumentListener: 조합이 완료된 문자에 색상 적용
        contentArea.getDocument().addDocumentListener(new DocumentListener() {
            private void applyColorToLastCharacter() {
                try {
                    StyledDocument doc = contentArea.getStyledDocument();
                    SimpleAttributeSet attrs = new SimpleAttributeSet();
                    StyleConstants.setForeground(attrs, currentTextColor.get());

                    int length = doc.getLength();
                    if (length > 0) {
                        doc.setCharacterAttributes(length - 1, 1, attrs, false);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        applyColorToLastCharacter();
                    }
                });
            }


            public void removeUpdate(DocumentEvent e) {
                // 삭제 시 처리 필요 없음
            }


            public void changedUpdate(DocumentEvent e) {
                // 스타일 변경 시 처리 필요 없음
            }
        });

        // 마우스 클릭 시 색상 유지
        contentArea.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                StyledDocument doc = contentArea.getStyledDocument();
                MutableAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, currentTextColor.get());
                contentArea.setCharacterAttributes(attrs, false);
            }
        });

        // 정렬 콤보박스
        JComboBox<String> alignmentComboBox = new JComboBox<>(new String[]{"좌로 정렬", "중앙 정렬", "우로 정렬"});
        alignmentComboBox.setBounds(50, 60, 120, 30);
        alignmentComboBox.setBackground(new Color(200, 162, 200)); // 배경색
        alignmentComboBox.setForeground(Color.BLACK); // 텍스트 색상
        alignmentComboBox.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrowButton = new JButton("▼");
                arrowButton.setFont(new Font("Arial", Font.BOLD, 12));
                arrowButton.setContentAreaFilled(false); // 버튼 채우기 제거
                arrowButton.setFocusPainted(false); // 포커스 표시 제거
                arrowButton.setBorder(BorderFactory.createEmptyBorder()); // 테두리 제거
                arrowButton.setBackground(new Color(200, 162, 200)); // 화살표 버튼 배경색
                arrowButton.setForeground(Color.WHITE); // 화살표 색상
                return arrowButton;
            }
        });
        
        alignmentComboBox.addActionListener(e -> {
            int selectedIndex = alignmentComboBox.getSelectedIndex(); // 선택된 인덱스 확인
            StyledDocument doc = contentArea.getStyledDocument(); // JTextPane의 문서 가져오기
            SimpleAttributeSet attributes = new SimpleAttributeSet();

            // 선택된 정렬에 따라 설정
            switch (selectedIndex) {
                case 0: // 좌로 정렬
                    StyleConstants.setAlignment(attributes, StyleConstants.ALIGN_LEFT);
                    break;
                case 1: // 중앙 정렬
                    StyleConstants.setAlignment(attributes, StyleConstants.ALIGN_CENTER);
                    break;
                case 2: // 우로 정렬
                    StyleConstants.setAlignment(attributes, StyleConstants.ALIGN_RIGHT);
                    break;
            }

            // 문서에 정렬 적용
            doc.setParagraphAttributes(0, doc.getLength(), attributes, false);
        });

        
        alignmentComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                renderer.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
                if (isSelected) {
                    renderer.setBackground(new Color(150, 122, 150)); // 선택된 항목 배경색
                    renderer.setForeground(Color.WHITE); // 선택된 항목 텍스트 색
                } else {
                    renderer.setBackground(Color.WHITE); // 비선택 배경색
                    renderer.setForeground(new Color(200, 162, 200)); // 비선택 텍스트 색
                }
                return renderer;
            }
        });
        diaryEditorPanel.add(alignmentComboBox);


        // 제목 입력 필드
        JTextField titleField = new JTextField(diary != null ? diary.getTitle() : "제목을 입력하세요");
        titleField.setBounds(10, 110, diaryEditorPanel.getWidth() - 20, 40);
        diaryEditorPanel.add(titleField);

        titleField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (titleField.getText().equals("제목을 입력하세요")) {
                    titleField.setText("");
                    titleField.setForeground(Color.BLACK);
                }
            }

            public void focusLost(FocusEvent e) {
                if (titleField.getText().isEmpty()) {
                    titleField.setText("제목을 입력하세요");
                    titleField.setForeground(Color.GRAY);
                }
            }
        });

        

        if (diary != null) {
            contentArea.setText(diary.getContent());
        } else {
            contentArea.setText("<html><body></body></html>");
        }

        // 실시간 색상 유지
        contentArea.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // 마우스로 클릭했을 때 텍스트 색상 유지
                StyledDocument doc = contentArea.getStyledDocument();
                MutableAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, currentTextColor.get());
                contentArea.setCharacterAttributes(attrs, false);
            }
        });

        JScrollPane scrollPane = new JScrollPane(contentArea);
        scrollPane.setBounds(10, 160, diaryEditorPanel.getWidth() - 20, diaryEditorPanel.getHeight() - 170);
        diaryEditorPanel.add(scrollPane);

        // 이미지 추가 버튼
        RoundedButton addImageButton = new RoundedButton("이미지");
        addImageButton.setBounds(180, 60, 100, 30);
        addImageButton.setBackground(new Color(200, 162, 200));
        addImageButton.setForeground(Color.WHITE);
        diaryEditorPanel.add(addImageButton);

        addImageButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(mainFrame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    String imagePath = selectedFile.getAbsolutePath();
                    String htmlImage = "<div style='margin:10px 0;'><img src='file:" + imagePath + "' width='300'></div>";
                    HTMLDocument doc = (HTMLDocument) contentArea.getDocument();
                    HTMLEditorKit editorKit = (HTMLEditorKit) contentArea.getEditorKit();

                    editorKit.insertHTML(doc, contentArea.getCaretPosition(), htmlImage, 0, 0, null);
                    contentArea.requestFocus();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainFrame, "이미지 추가 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 스티커 버튼
        RoundedButton stickerButton = new RoundedButton("스티커");
        stickerButton.setBounds(290, 60, 100, 30);
        stickerButton.setBackground(new Color(200, 162, 200));
        stickerButton.setForeground(Color.WHITE);
        diaryEditorPanel.add(stickerButton);

        stickerButton.addActionListener(e -> createStickerListUI(0)); // 스티커 목록으로 이동

        
        RoundedButton saveButton = new RoundedButton("저장");
        saveButton.setBackground(Color.WHITE);
        saveButton.setForeground(new Color(200, 162, 200));
        saveButton.setBounds(diaryEditorPanel.getWidth() - 70, 10, 60, 30);
        headerPanel.add(saveButton);

        
        
        saveButton.addActionListener(e -> {
            try {
                String title = titleField.getText().trim();
                HTMLDocument doc = (HTMLDocument) contentArea.getDocument();
                StringWriter writer = new StringWriter();
                new HTMLEditorKit().write(writer, doc, 0, doc.getLength());
                String finalHtmlContent = sanitizeHtmlForSave(writer.toString());

                if (!title.isEmpty() && !finalHtmlContent.isEmpty()) {
                    if (diary == null) {
                        // 새 일기 작성
                        DiaryEntry newDiary = new DiaryEntry(title, finalHtmlContent, LocalDate.now().toString());
                        userDiaryDatabase.get(currentUserId).add(newDiary); // 사용자 데이터에 저장
                        diaryDatabase.add(newDiary); // 현재 세션의 데이터베이스에도 추가
                    } else {
                        // 기존 일기 수정
                        diary.setTitle(title);
                        diary.setContent(finalHtmlContent);

                        // 현재 세션의 데이터베이스에서도 업데이트
                        int diaryIndex = diaryDatabase.indexOf(diary);
                        if (diaryIndex != -1) {
                            diaryDatabase.set(diaryIndex, diary);
                        }
                    }

                    saveUserData(); // 사용자 데이터 저장
                    JOptionPane.showMessageDialog(mainFrame, "일기가 저장되었습니다.", "저장 완료", JOptionPane.INFORMATION_MESSAGE);
                    createDiaryListUI(); // 목록 화면으로 이동
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "제목과 내용을 모두 입력해주세요.", "저장 실패", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainFrame, "저장 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });




        switchPanel(diaryEditorPanel);
    }



    // 텍스트 정렬 메서드
    private void setAlignment(int alignment) {
        StyledDocument doc = contentArea.getStyledDocument();
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setAlignment(attr, alignment);
        doc.setParagraphAttributes(0, doc.getLength(), attr, false);
    }


 
    private String sanitizeHtmlForSave(String rawHtml) {
        return rawHtml
            .replaceAll("\n", "<br>") // 줄바꿈 문자 -> <br> 태그
            .replaceAll("(?i)<head>.*?</head>", "") // <head> 태그 제거
            .replaceAll("(?i)<style>.*?</style>", "") // <style> 태그 제거
            .replaceAll("(?i)<body>", "") // <body> 태그 제거
            .replaceAll("(?i)</body>", "") // </body> 태그 제거
            .replaceAll("(?i)<html>", "") // <html> 태그 제거
            .replaceAll("(?i)</html>", "") // </html> 태그 제거
            .replaceAll("(<br>\\s*)+", "<br>") // 중복된 <br> 태그 제거
            .trim(); // 양쪽 공백 제거
    }


    private String sanitizeHtmlForDisplay(String rawHtml) {
        return rawHtml
            .replaceAll("(?i)<head>.*?</head>", "") // <head> 태그 제거
            .replaceAll("(?i)<style>.*?</style>", "") // <style> 태그 제거
            .replaceAll("(?i)<body>", "") // <body> 태그 제거
            .replaceAll("(?i)</body>", "") // </body> 태그 제거
            .replaceAll("(?i)<html>", "") // <html> 태그 제거
            .replaceAll("(?i)</html>", "") // </html> 태그 제거
            .replaceAll("\\s*(<br>\\s*)+", "<br>") // 중복된 <br> 제거
            .trim(); // 양쪽 공백 제거
    }

    private String sanitizeHtmlForEdit(String html) {
        return html
            .replaceAll("(?i)<br>", "\n") // <br> -> 줄바꿈 문자
            .replaceAll("(?i)<[^>]*>", "") // HTML 태그 제거
            .trim(); // 양쪽 공백 제거
    }

    
    private void createStickerListUI(int currentPageIndex) {
        BasePanel stickerListPanel = new BasePanel(Color.WHITE);
        AtomicInteger currentPage = new AtomicInteger(currentPageIndex);

        // 상단 패널
        JPanel topPanel = new JPanel(null);
        topPanel.setBounds(0, 0, stickerListPanel.getWidth(), 50);
        topPanel.setBackground(new Color(200, 160, 205));
        stickerListPanel.add(topPanel);

        JLabel backLabel = new JLabel("←", SwingConstants.LEFT);
        backLabel.setFont(backLabel.getFont().deriveFont(20f));
        backLabel.setBounds(10, 10, 50, 30);
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        topPanel.add(backLabel);
        backLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                switchPanel(diaryEditorPanel);
            }
        });

        JLabel titleLabel = new JLabel(userDatabase.get(currentUserId).getName() + "님의 이모티콘 목록", SwingConstants.CENTER);
        titleLabel.setBounds((stickerListPanel.getWidth() - 200) / 2, 10, 200, 30);
        topPanel.add(titleLabel);

        JLabel addStickerLabel = new JLabel("⨁", SwingConstants.RIGHT);
        addStickerLabel.setFont(addStickerLabel.getFont().deriveFont(20f));
        addStickerLabel.setBounds(stickerListPanel.getWidth() - 60, 10, 40, 30);
        addStickerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        topPanel.add(addStickerLabel);

        addStickerLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String title = JOptionPane.showInputDialog(
                    stickerListPanel, "제목을 입력하세요:", "새 이모티콘 제목", JOptionPane.PLAIN_MESSAGE
                );

                if (title != null && !title.trim().isEmpty()) {
                    // 중복 확인
                    if (stickerTitles.contains(title.trim())) {
                        JOptionPane.showMessageDialog(
                            stickerListPanel, "같은 제목이 이미 존재합니다. 다른 제목을 입력하세요.", "오류", JOptionPane.ERROR_MESSAGE
                        );
                    } else {
                        // 중복이 아니면 추가
                        stickerTitles.add(title.trim());
                        createDrawingBoardUI(title.trim());
                    }
                } else {
                    JOptionPane.showMessageDialog(
                        stickerListPanel, "제목을 입력해주세요.", "경고", JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        });


        // 스티커 목록 그리드
        JPanel stickerGrid = new JPanel(null);
        int gridWidth = stickerListPanel.getWidth();
        int gridHeight = stickerListPanel.getHeight() - 110;
        int bottomPanelHeight = 40;
        stickerGrid.setBounds(10, 60, gridWidth - 20, gridHeight);
        stickerGrid.setBackground(Color.WHITE);
        stickerListPanel.add(stickerGrid);

        int cellWidth = gridWidth / 3 - 20;
        int cellHeight = (gridHeight / 5) - 20;
        int gap = 15;

        int start = currentPage.get() * 15;
        int end = Math.min(start + 15, stickerDatabase.size());

        int totalPages = Math.max(1, (int) Math.ceil(stickerDatabase.size() / 15.0));
        if (currentPage.get() >= totalPages) {
            currentPage.set(0); // 현재 페이지를 1페이지로 변경
            start = 0; // 새롭게 로드할 데이터의 시작점
            end = Math.min(15, stickerDatabase.size());
        }

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 3; col++) {
                int stickerIndex = row * 3 + col + start;
                JPanel cell = new JPanel();
                cell.setBounds(col * (cellWidth + gap), row * (cellHeight + gap), cellWidth, cellHeight);
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3));

                if (stickerIndex < end) {
                    // 스티커 데이터 로드
                    String stickerPath = stickerDatabase.get(stickerIndex); // 스티커 경로 가져오기
                    BufferedImage sticker = loadBufferedImageFromFile(stickerPath); // 경로에서 이미지 로드
                    if (sticker != null) {
                        ImageIcon icon = new ImageIcon(sticker.getScaledInstance(cellWidth - 20, cellHeight - 20, Image.SCALE_SMOOTH));
                        JLabel stickerLabel = new JLabel(icon);
                        stickerLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        stickerLabel.setVerticalAlignment(SwingConstants.CENTER);
                        cell.add(stickerLabel);

                        stickerLabel.addMouseListener(new MouseAdapter() {
                            public void mouseClicked(MouseEvent e) {
                                if (SwingUtilities.isLeftMouseButton(e)) {
                                    // 왼쪽 클릭: 일기에 스티커 추가
                                    addStickerToDiaryEditor(stickerPath); // 스티커 경로를 전달
                                    switchPanel(diaryEditorPanel); // 일기 편집 화면으로 이동
                                } else if (SwingUtilities.isRightMouseButton(e)) {
                                    // 오른쪽 클릭: 스티커 삭제
                                    String stickerTitle = stickerTitles.get(stickerIndex);
                                    int confirm = JOptionPane.showConfirmDialog(
                                            stickerListPanel,
                                            stickerTitle + "를 삭제하시겠습니까?",
                                            "삭제 확인",
                                            JOptionPane.YES_NO_OPTION
                                    );
                                    if (confirm == JOptionPane.YES_OPTION) {
                                        // 데이터 삭제
                                        stickerDatabase.remove(stickerIndex);
                                        stickerTitles.remove(stickerIndex);

                                        saveUserData(); // 변경된 데이터 저장

                                        createStickerListUI(currentPage.get()); // UI 갱신
                                    }
                                }
                            }
                        });


                    }
                }

                stickerGrid.add(cell);
            }
        }


        // 하단 페이지네이션
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBounds(10, stickerListPanel.getHeight() - bottomPanelHeight, gridWidth - 20, bottomPanelHeight);
        bottomPanel.setBackground(new Color(240, 240, 240));
        stickerListPanel.add(bottomPanel);

        for (int i = 0; i < totalPages; i++) {
            JLabel pageLabel = new JLabel(String.valueOf(i + 1));
            pageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if (i == currentPage.get()) {
                pageLabel.setText("<html><b>" + (i + 1) + "</b></html>");
            }

            final int pageIndex = i;
            pageLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    currentPage.set(pageIndex);
                    createStickerListUI(currentPage.get());
                }
            });

            bottomPanel.add(pageLabel);
        }

        switchPanel(stickerListPanel);
    }


    private BufferedImage loadBufferedImageFromFile(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "이미지 파일을 불러올 수 없습니다: " + path, "오류", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }


    private void addStickerToDiaryEditor(String stickerPath) {
        if (contentArea == null) {
            JOptionPane.showMessageDialog(mainFrame, "일기 작성 화면이 초기화되지 않았습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BufferedImage sticker = loadBufferedImageFromFile(stickerPath); // 파일에서 로드
        if (sticker == null) {
            JOptionPane.showMessageDialog(mainFrame, "스티커를 로드할 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 이미지 경로를 임시 파일로 저장하여 일기에 삽입
        String imagePath = "file:" + saveStickerToTempFile(sticker);
        String htmlImage = "<img src='" + imagePath + "' width='100' height='100'>";

        try {
            HTMLDocument doc = (HTMLDocument) contentArea.getDocument();
            HTMLEditorKit editorKit = (HTMLEditorKit) contentArea.getEditorKit();
            editorKit.insertHTML(doc, contentArea.getCaretPosition(), htmlImage, 0, 0, null);
            contentArea.requestFocus(); // 포커스 설정
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String saveStickerToTempFile(BufferedImage sticker) {
        try {
            File tempFile = File.createTempFile("sticker", ".png");
            ImageIO.write(sticker, "png", tempFile);
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }



    private String saveBufferedImageToFile(BufferedImage image, String title) {
        try {
            // 스티커 저장 디렉토리 생성
            File stickerDir = new File("stickers");
            if (!stickerDir.exists()) {
                stickerDir.mkdir();
            }

            // 파일 경로 생성
            File file = new File(stickerDir, title + ".png");
            ImageIO.write(image, "png", file);
            return file.getAbsolutePath(); // 파일 경로 반환
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }



    private void createDrawingBoardUI(String title) {
        BasePanel drawingBoardPanel = new BasePanel(Color.WHITE); // 배경색 설정

        // 캔버스 크기와 초기화
        int boardSize = 350;
        BufferedImage canvasImage = new BufferedImage(boardSize, boardSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = canvasImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, boardSize, boardSize);
        g2d.dispose();

        // 현재 도구, 색상 및 펜 두께
        AtomicReference<String> currentTool = new AtomicReference<>("펜");
        AtomicReference<Color> selectedColor = new AtomicReference<>(Color.BLACK);
        AtomicReference<Integer> penThickness = new AtomicReference<>(10); // 기본 두께 10

        // 캔버스 (중앙 정렬)
        JLabel drawingCanvas = new JLabel(new ImageIcon(canvasImage));
        int canvasX = (drawingBoardPanel.getWidth() - boardSize) / 2; // 가로 중앙
        drawingCanvas.setBounds(canvasX, 100, boardSize, boardSize); // Y 위치: 툴바 아래
        drawingCanvas.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        drawingBoardPanel.add(drawingCanvas);

        // 상단 패널
        JPanel topPanel = new JPanel(null);
        topPanel.setBounds(0, 0, drawingBoardPanel.getWidth(), 50);
        topPanel.setBackground(new Color(200, 160, 205));
        drawingBoardPanel.add(topPanel);

        JLabel backLabel = new JLabel("←", SwingConstants.LEFT);
        backLabel.setFont(backLabel.getFont().deriveFont(20f));
        backLabel.setBounds(10, 10, 50, 30);
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        topPanel.add(backLabel);
        backLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                createStickerListUI(0); // 이전 화면으로 이동
            }
        });

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setBounds((drawingBoardPanel.getWidth() - 200) / 2, 10, 200, 30);
        topPanel.add(titleLabel);

        JButton saveButton = new RoundedButton("저장");
        saveButton.setBackground(Color.WHITE);
        saveButton.setForeground(new Color(200, 162, 200));
        saveButton.setBounds(drawingBoardPanel.getWidth() - 80, 10, 70, 30);
        topPanel.add(saveButton);

        // 툴바
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setBounds(10, 60, drawingBoardPanel.getWidth() - 20, 30);
        toolbar.setBackground(new Color(230, 230, 230));
        drawingBoardPanel.add(toolbar);

        String[] tools = {"펜", "선", "네모", "원", "지우개", "전체 지우기", "펜 두께: 10"};
        for (String tool : tools) {
            JLabel toolLabel = new JLabel(tool);
            toolLabel.setFont(toolLabel.getFont().deriveFont(16f));
            toolLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toolbar.add(toolLabel);

            toolLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (tool.startsWith("펜 두께")) {
                        JComboBox<Integer> thicknessComboBox = new JComboBox<>(
                            IntStream.rangeClosed(1, 100).boxed().toArray(Integer[]::new)
                        );
                        thicknessComboBox.setSelectedItem(penThickness.get()); // 현재 두께로 설정
                        int result = JOptionPane.showConfirmDialog(
                            drawingBoardPanel,
                            thicknessComboBox,
                            "펜 두께 설정",
                            JOptionPane.OK_CANCEL_OPTION
                        );
                        if (result == JOptionPane.OK_OPTION) {
                            int selectedThickness = (int) thicknessComboBox.getSelectedItem();
                            penThickness.set(selectedThickness);
                            toolLabel.setText("펜 두께: " + selectedThickness); // 두께 표시 업데이트
                        }
                    } else if (tool.equals("전체 지우기")) {
                        Graphics2D g = canvasImage.createGraphics();
                        g.setColor(Color.WHITE);
                        g.fillRect(0, 0, boardSize, boardSize);
                        g.dispose();
                        drawingCanvas.repaint();
                    } else {
                        currentTool.set(tool);
                    }
                }
            });
        }

        // 색상 선택 패널 (캔버스 아래)
        JPanel colorPanel = new JPanel(new FlowLayout());
        colorPanel.setBounds(10, 460, drawingBoardPanel.getWidth() - 20, 50);
        drawingBoardPanel.add(colorPanel);

        Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, new Color(75, 0, 130), new Color(238, 130, 238)};
        for (Color color : colors) {
            JLabel colorCircle = new JLabel("●");
            colorCircle.setFont(colorCircle.getFont().deriveFont(30f));
            colorCircle.setForeground(color);
            colorCircle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            colorPanel.add(colorCircle);

            colorCircle.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    selectedColor.set(color);
                }
            });
        }

        JLabel rainbowLabel = new JLabel("🌈");
        rainbowLabel.setFont(rainbowLabel.getFont().deriveFont(30f));
        rainbowLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        colorPanel.add(rainbowLabel);

        rainbowLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Color newColor = JColorChooser.showDialog(drawingBoardPanel, "색상 선택", selectedColor.get());
                if (newColor != null) {
                    selectedColor.set(newColor);
                }
            }
        });

        // 저장 버튼
        saveButton.addActionListener(e -> {
            try {
                BufferedImage resizedCanvas = new BufferedImage(boardSize, boardSize, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = resizedCanvas.createGraphics();
                g.drawImage(canvasImage, 0, 0, boardSize, boardSize, null);
                g.dispose();

                // 스티커 데이터를 파일로 저장
                String imagePath = saveBufferedImageToFile(resizedCanvas, title);

                if (imagePath != null) {
                    stickerTitles.add(title);
                    stickerDatabase.add(imagePath); // 파일 경로 저장

                    saveUserData(); // 사용자 데이터 저장

                    JOptionPane.showMessageDialog(drawingBoardPanel, "스티커가 저장되었습니다!", "저장 완료", JOptionPane.INFORMATION_MESSAGE);
                    createStickerListUI(0);
                } else {
                    JOptionPane.showMessageDialog(drawingBoardPanel, "스티커 저장에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(drawingBoardPanel, "스티커 저장 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });



        // 드로잉 기능
        drawingCanvas.addMouseListener(new MouseAdapter() {
            Point startPoint = new Point();

            public void mousePressed(MouseEvent e) {
                startPoint.setLocation(e.getPoint());
            }

            public void mouseReleased(MouseEvent e) {
                Graphics2D g = canvasImage.createGraphics();
                g.setColor(selectedColor.get());
                g.setStroke(new BasicStroke(penThickness.get())); // 펜 두께 적용
                Point endPoint = e.getPoint();

                switch (currentTool.get()) {
                    case "선":
                        g.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
                        break;
                    case "네모":
                        g.drawRect(Math.min(startPoint.x, endPoint.x), Math.min(startPoint.y, endPoint.y),
                                Math.abs(startPoint.x - endPoint.x), Math.abs(startPoint.y - endPoint.y));
                        break;
                    case "원":
                        g.drawOval(Math.min(startPoint.x, endPoint.x), Math.min(startPoint.y, endPoint.y),
                                Math.abs(startPoint.x - endPoint.x), Math.abs(startPoint.y - endPoint.y));
                        break;
                }
                g.dispose();
                drawingCanvas.repaint();
            }
        });

        drawingCanvas.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if ("펜".equals(currentTool.get()) || "지우개".equals(currentTool.get())) {
                    Graphics2D g = canvasImage.createGraphics();
                    g.setColor("지우개".equals(currentTool.get()) ? Color.WHITE : selectedColor.get());
                    g.setStroke(new BasicStroke(penThickness.get())); // 펜 두께 적용
                    g.drawLine(e.getX(), e.getY(), e.getX(), e.getY());
                    g.dispose();
                    drawingCanvas.repaint();
                }
            }
        });

        switchPanel(drawingBoardPanel);
    }





    
    private void createSettingsUI() {
        BasePanel settingsPanel = new BasePanel(Color.DARK_GRAY);

        JLabel backLabel = new JLabel("←", SwingConstants.LEFT);
        backLabel.setFont(backLabel.getFont().deriveFont(30f));
        backLabel.setForeground(Color.WHITE);
        backLabel.setBounds(10, 10, 50, 40);
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsPanel.add(backLabel);

        backLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                createCalendarUI();
            }
        });

        JLabel titleLabel = new JLabel("Setting", SwingConstants.CENTER);
        titleLabel.setBounds(0, 20, settingsPanel.getWidth(), 40);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        settingsPanel.add(titleLabel);

        // 사용자 이미지
        CircleImageLabel userImageLabel = new CircleImageLabel(userDatabase.get(currentUserId).getImagePath());
        userImageLabel.setBounds((settingsPanel.getWidth() - 150) / 2, 80, 150, 150);
        settingsPanel.add(userImageLabel);

        userImageLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(mainFrame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    userDatabase.get(currentUserId).setImagePath(selectedFile.getAbsolutePath());
                    userImageLabel.setImagePath(selectedFile.getAbsolutePath());
                    userImageLabel.repaint();
                }
            }
        });

        
        // 사용자 이름 변경
        JLabel userNameLabel = new JLabel(userDatabase.get(currentUserId).getName(), SwingConstants.CENTER);
        userNameLabel.setForeground(Color.WHITE);
        userNameLabel.setBounds(0, 240, settingsPanel.getWidth(), 30);
        settingsPanel.add(userNameLabel);

        JLabel changeNameLabel = new JLabel("+ 이름 변경", SwingConstants.LEFT);
        changeNameLabel.setForeground(Color.WHITE);
        changeNameLabel.setBounds(30, 300, 200, 30);
        changeNameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsPanel.add(changeNameLabel);

        RoundedTextField nameField = new RoundedTextField("새 이름을 입력하세요");
        nameField.setBounds(30, 340, 240, 40);
        nameField.setVisible(false);
        settingsPanel.add(nameField);

        RoundedButton saveNameButton = new RoundedButton("저장");
        saveNameButton.setBounds(280, 340, 80, 40);
        saveNameButton.setBackground(Color.GRAY);
        saveNameButton.setForeground(Color.WHITE);
        saveNameButton.setVisible(false);
        settingsPanel.add(saveNameButton);

        changeNameLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                boolean isVisible = nameField.isVisible();
                nameField.setVisible(!isVisible);
                saveNameButton.setVisible(!isVisible);

                if (!isVisible) {
                    nameField.setText("새 이름을 입력하세요");
                    nameField.setForeground(Color.GRAY);
                }
            }
        });

        saveNameButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            if (!newName.isEmpty() && !newName.equals("새 이름을 입력하세요")) {
                userDatabase.get(currentUserId).setName(newName);
                userNameLabel.setText(newName);
                nameField.setVisible(false);
                saveNameButton.setVisible(false);
                saveUserData(); // 변경된 데이터를 저장
                save("user_data.ser"); // 공통 데이터 저장
                JOptionPane.showMessageDialog(mainFrame, "이름이 변경되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "유효한 이름을 입력하세요.", "오류", JOptionPane.WARNING_MESSAGE);
            }
        });

        // 비밀번호 변경
        JLabel changePasswordLabel = new JLabel("+ 비밀번호 재설정", SwingConstants.LEFT);
        changePasswordLabel.setForeground(Color.WHITE);
        changePasswordLabel.setBounds(30, 400, 200, 30);
        changePasswordLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsPanel.add(changePasswordLabel);

        RoundedPasswordField currentPasswordField = new RoundedPasswordField("기존 비밀번호");
        currentPasswordField.setBounds(30, 440, 300, 40);
        currentPasswordField.setVisible(false);
        settingsPanel.add(currentPasswordField);

        RoundedPasswordField newPasswordField = new RoundedPasswordField("새 비밀번호");
        newPasswordField.setBounds(30, 490, 300, 40);
        newPasswordField.setVisible(false);
        settingsPanel.add(newPasswordField);

        RoundedButton savePasswordButton = new RoundedButton("저장");
        savePasswordButton.setBounds(30, 540, 300, 40);
        savePasswordButton.setBackground(Color.GRAY);
        savePasswordButton.setForeground(Color.WHITE);
        savePasswordButton.setVisible(false);
        settingsPanel.add(savePasswordButton);

        changePasswordLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean isVisible = currentPasswordField.isVisible();
                currentPasswordField.setVisible(!isVisible);
                newPasswordField.setVisible(!isVisible);
                savePasswordButton.setVisible(!isVisible);

                if (!isVisible) {
                    currentPasswordField.setText("기존 비밀번호");
                    currentPasswordField.setForeground(Color.GRAY);
                    currentPasswordField.setEchoChar((char) 0);

                    newPasswordField.setText("새 비밀번호");
                    newPasswordField.setForeground(Color.GRAY);
                    newPasswordField.setEchoChar((char) 0);
                }
            }
        });

        currentPasswordField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (currentPasswordField.getText().equals("기존 비밀번호")) {
                    currentPasswordField.setText("");
                    currentPasswordField.setForeground(Color.GRAY);
                    currentPasswordField.setEchoChar('●');
                }
            }

            public void focusLost(FocusEvent e) {
                if (currentPasswordField.getText().isEmpty()) {
                    currentPasswordField.setText("기존 비밀번호");
                    currentPasswordField.setForeground(Color.GRAY);
                    currentPasswordField.setEchoChar((char) 0);
                }
            }
        });

        newPasswordField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (newPasswordField.getText().equals("새 비밀번호")) {
                    newPasswordField.setText("");
                    newPasswordField.setForeground(Color.GRAY);
                    newPasswordField.setEchoChar('●');
                }
            }

            public void focusLost(FocusEvent e) {
                if (newPasswordField.getText().isEmpty()) {
                    newPasswordField.setText("새 비밀번호");
                    newPasswordField.setForeground(Color.GRAY);
                    newPasswordField.setEchoChar((char) 0);
                }
            }
        });

        savePasswordButton.addActionListener(e -> {
            String currentPassword = new String(currentPasswordField.getPassword()).trim();
            String newPassword = new String(newPasswordField.getPassword()).trim();

            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "모든 필드를 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!userDatabase.get(currentUserId).getPassword().equals(currentPassword)) {
                JOptionPane.showMessageDialog(mainFrame, "기존 비밀번호가 일치하지 않습니다.", "비밀번호 변경 실패", JOptionPane.ERROR_MESSAGE);
                return;
            }

            userDatabase.get(currentUserId).setPassword(newPassword);
            saveUserData(); // 변경된 데이터를 저장
            save("user_data.ser"); // 공통 데이터 저장
            JOptionPane.showMessageDialog(mainFrame, "비밀번호가 성공적으로 변경되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);

            currentPasswordField.setVisible(false);
            newPasswordField.setVisible(false);
            savePasswordButton.setVisible(false);
        });

        // 로그아웃 버튼
        LogoutButton logoutButton = new LogoutButton("로그아웃");
        logoutButton.setBounds(50, settingsPanel.getHeight() - 80, 300, 40);
        logoutButton.setBackground(new Color(80, 80, 80));
        logoutButton.setForeground(Color.RED);
        settingsPanel.add(logoutButton);

        logoutButton.addActionListener(e -> {
            saveUserData(); // 현재 사용자 데이터 저장
            currentUserId = null; // 현재 사용자 초기화
            JOptionPane.showMessageDialog(mainFrame, "로그아웃 완료.");
            createLoginUI(); // 로그인 화면으로 이동
        });


        JLabel deleteAccountLabel = new JLabel("계정 삭제", SwingConstants.RIGHT);
        deleteAccountLabel.setForeground(Color.GRAY);
        deleteAccountLabel.setBounds(settingsPanel.getWidth() - 140, settingsPanel.getHeight() - 40, 90, 20);
        deleteAccountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsPanel.add(deleteAccountLabel);

        deleteAccountLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int confirm = JOptionPane.showConfirmDialog(mainFrame, "계정을 삭제하시겠습니까?", "계정 삭제", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    // 사용자 데이터 삭제
                    userDatabase.remove(currentUserId);
                    currentUserId = null;

                    // 사용자 데이터 파일 삭제
                    File userFile = new File("user_" + currentUserId + ".ser");
                    if (userFile.exists()) {
                        userFile.delete();
                    }

                    save("user_data.ser"); // 공통 데이터 업데이트
                    createLoginUI(); // 로그인 화면으로 이동
                }
            }
        });


        

        switchPanel(settingsPanel);
    }



    class LogoutButton extends JButton {
        public LogoutButton(String text) {
            super(text);
            setContentAreaFilled(false); // 배경색 비활성화
            setFocusPainted(false);      // 포커스 박스 비활성화
            setBorderPainted(false);     // 기본 테두리 비활성화
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 배경색
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

            g2.dispose();
            super.paintComponent(g); // 텍스트 등 기본 컴포넌트 그리기
        }

        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 흰색 테두리
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

            g2.dispose();
        }
    }




    public static void main(String[] args) {
        DecorTime decorTime;
        File dataFile = new File("user_data.ser");

        if (dataFile.exists()) {
            decorTime = DecorTime.load("user_data.ser");
            if (decorTime != null) {
                decorTime.initializeMainFrame();
                decorTime.createLoginUI();
            } else {
                decorTime = new DecorTime();
            }
        } else {
            decorTime = new DecorTime();
            decorTime.printUserDatabase(); // 빈 사용자 목록 출력
        }
    }

}


class User implements Serializable {
    private static final long serialVersionUID = 1L; // 직렬화 버전 관리 ID
    private String id;
    private String password;
    private String name;
    private String imagePath;

    // 기존 코드 그대로 유지
    public User(String id, String password, String name, String imagePath) {
        this.id = id;
        this.password = password;
        this.name = name;
        this.imagePath = imagePath;
    }

    public String getId() {
        return id;
    }
    
    // Getter/Setter 유지
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}

class DiaryEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private String content;
    private String date;

    public DiaryEntry(String title, String content, String date) {
        this.title = title;
        this.content = content;
        this.date = date;
    }

    // Getter/Setter 유지
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }
}



class DrawingCanvas extends JPanel {
    private BufferedImage image;
    private Graphics2D g2d;
    private int startX, startY, endX, endY;
    private Color currentColor = Color.BLACK;

    public DrawingCanvas() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(400, 400));

        image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(currentColor);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                startX = e.getX();
                startY = e.getY();
            }

            public void mouseReleased(MouseEvent e) {
                endX = e.getX();
                endY = e.getY();
                g2d.drawLine(startX, startY, endX, endY); // 기본 도형: 선
                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                endX = e.getX();
                endY = e.getY();
                g2d.drawLine(startX, startY, endX, endY); // 실시간 선 그리기
                startX = endX;
                startY = endY;
                repaint();
            }
        });
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null);
    }

    public void saveToFile(String filePath) throws Exception {
        ImageIO.write(image, "png", new File(filePath));
    }
}

class RoundedColorButton extends JButton {
    public RoundedColorButton(Color backgroundColor) {
        super();
        setContentAreaFilled(false); // 배경 채우기 비활성화
        setFocusPainted(false);      // 포커스 박스 비활성화
        setBorderPainted(false);     // 테두리 비활성화
        setBackground(backgroundColor); // 버튼 배경 색상 설정
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillOval(0, 0, getWidth(), getHeight()); // 완전한 동그라미
        g2.dispose();
        super.paintComponent(g);
    }

    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE); // 테두리 색상 (흰색)
        g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1); // 테두리 그리기
        g2.dispose();
    }
}

class CircleImageLabel extends JLabel {
    private Image image;
    private String imagePath;

    public CircleImageLabel(String imagePath) {
        setImagePath(imagePath);
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
        File file = new File(imagePath);
        if (file.exists()) {
            this.image = new ImageIcon(imagePath).getImage();
        } else {
            this.image = null;
        }
    }

    public String getImagePath() {
        return imagePath;
    }

    protected void paintComponent(Graphics g) {
        if (image != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            int width = getWidth();
            int height = getHeight();
            int diameter = Math.min(width, height);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, diameter, diameter));
            g2d.drawImage(image, 0, 0, width, height, this);
            g2d.setClip(null); // 클립 해제
            g2d.setColor(new Color(200, 162, 200)); 
            g2d.setStroke(new BasicStroke(5));
            g2d.drawOval(0, 0, diameter - 1, diameter - 1);
            g2d.dispose();
        } else {
            super.paintComponent(g);
        }
    }
}

