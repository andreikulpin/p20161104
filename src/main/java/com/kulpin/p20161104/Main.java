package com.kulpin.p20161104;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.ss.usermodel.Row;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;

public class Main extends JFrame{
    private JButton buttonOpenFile;
    private JButton buttonSave;
    private JList listCosts;
    private JLabel textRate;
    private JLabel textPriceCost;

    DefaultListModel<BigDecimal> listModel;
    private float supplierRate = -1f;
    private BigDecimal priceCost;

    public Main() throws HeadlessException {
        super("Project");
        initComponents();
    }

    public static void main(String[] args) {
        UIManager.put("Label.font", new javax.swing.plaf.FontUIResource("Arial",Font.TYPE1_FONT,20));
        UIManager.put("Button.font", new javax.swing.plaf.FontUIResource("Arial",Font.TYPE1_FONT,16));
        UIManager.put("List.font", new javax.swing.plaf.FontUIResource("Arial", Font.TYPE1_FONT, 24));

        Main frame = new Main();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }

    /*
    * Добавляет необходимые компоненты интерфейса в JFrame.
    * */
    private void initComponents(){
        Box box = Box.createVerticalBox();
        setContentPane(box);

        //Кнопка, вызывающая диалог выбора файла
        JPanel panel = new JPanel();
        buttonOpenFile = new JButton("Open Excel document");
        buttonOpenFile.setPreferredSize(new Dimension(240, 30));
        buttonOpenFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooseFile();
            }
        });
        panel.add(buttonOpenFile);
        add(panel);

        add(Box.createVerticalStrut(20));

        //Заголовки
        JPanel grid = new JPanel();
        grid.setLayout(new GridLayout(1, 3, 40, 5));
        grid.add(new JLabel("Costs", JLabel.CENTER));
        grid.add(new JLabel("Supplier Rate", JLabel.CENTER));
        grid.add(new JLabel("Price Cost", JLabel.CENTER));
        add(grid);

        add(Box.createVerticalStrut(10));

        grid = new JPanel();
        grid.setLayout(new GridLayout(1, 3, 40, 5));

        //Окно списка
        panel = new JPanel();
        listCosts = new JList();
        listCosts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listCosts.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    computePriceCost();
                }
            }
        });
        listModel = new DefaultListModel();
        listCosts.setModel(listModel);
        JPanel listPanel = new JPanel();
        listPanel.setBackground(Color.white);
        listPanel.setBorder(new LineBorder(Color.BLACK, 2));
        listPanel.setPreferredSize(new Dimension(150, 150));
        listPanel.add(listCosts);
        panel.add(listPanel);
        grid.add(panel);

        //Окно процента поставщика
        panel = new JPanel();
        JPanel textPanel = new JPanel();
        textPanel.setBackground(Color.white);
        textPanel.setBorder(new LineBorder(Color.BLACK, 2));
        textPanel.setPreferredSize(new Dimension(100, 40));
        textRate = new JLabel("");
        textPanel.add(textRate);
        textPanel.addMouseListener(new MouseClickListener(this));
        panel.add(textPanel, JPanel.CENTER_ALIGNMENT);
        grid.add(panel, JPanel.CENTER_ALIGNMENT);

        //Окно значения себестоимости
        panel = new JPanel();
        textPanel = new JPanel();
        textPanel.setBackground(Color.white);
        textPanel.setBorder(new LineBorder(Color.BLACK, 2));
        textPanel.setPreferredSize(new Dimension(150, 40));
        textPriceCost = new JLabel("");
        textPanel.add(textPriceCost);
        panel.add(textPanel, JPanel.CENTER_ALIGNMENT);
        grid.add(panel, JPanel.CENTER_ALIGNMENT);
        add(grid);

        add(Box.createVerticalStrut(20));

        //Кнопка "сохранить"
        panel = new JPanel();
        buttonSave = new JButton("Save");
        buttonSave.setPreferredSize(new Dimension(240, 30));
        buttonSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DBHelper.saveData(listModel.get(listCosts.getSelectedIndex()), supplierRate, priceCost);
            }
        });
        buttonSave.setEnabled(false);
        panel.add(buttonSave);
        add(panel, BorderLayout.SOUTH);
    }

    /*
    * Считает значение себестоимости по выбранному значению из списка затрат
    * и значению процента поставщика. Необходимо, чтобы был выбран элемент списка
    * и введено значение процента.
    * */
    private void computePriceCost(){
        if (listCosts.getSelectedIndex() >= 0 && supplierRate >= 0){
            BigDecimal cost = listModel.get(listCosts.getSelectedIndex());
            BigDecimal suppliersPart = cost.multiply(BigDecimal.valueOf(supplierRate / 100).setScale(4, BigDecimal.ROUND_HALF_UP));
            priceCost = cost.add(suppliersPart);
            textPriceCost.setText("" + priceCost.stripTrailingZeros());
            buttonSave.setEnabled(true);
        }
    }

    /*
    * Открывает диалог выбора файла
    * */
    public void chooseFile(){
        JFileChooser fileChooser = new JFileChooser();
        int ret = fileChooser.showDialog(null, "Open file");
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            int i;
            if ((i = file.getName().lastIndexOf('.')) > 0 && !file.getName().substring(i + 1).toLowerCase().equals("xls")){
                JOptionPane.showMessageDialog(this, "Not an .xls file", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                readExcelFile(file);
            } catch (FileNotFoundException e1) {
            } catch (IOException e1) {
            }
        }
    }

    /*
    * Читает значения первого столбца первого листа выбранного Excel-документа
    * и заполняет listCosts.
    * */
    public void readExcelFile(File file) throws FileNotFoundException, IOException{
        try {
            HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));

            Iterator<Row> iterator = workbook.getSheetAt(0).rowIterator();

            if (!iterator.hasNext()) {
                JOptionPane.showMessageDialog(this, "Sheet is empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            else listModel.clear();

            while (iterator.hasNext()) {
                HSSFRow itRow = (HSSFRow) iterator.next();
                if (itRow.getCell(0) != null && itRow.getCell(0).getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                    listModel.addElement(BigDecimal.valueOf( itRow.getCell(0).getNumericCellValue()));
                }
            }

            if (listModel.isEmpty()){
                JOptionPane.showMessageDialog(this, "Values not found", "Error", JOptionPane.ERROR_MESSAGE);
            }

        }catch (OfficeXmlFileException e){
            JOptionPane.showMessageDialog(this, "Wrong document format", "Error", JOptionPane.ERROR_MESSAGE);
        }

    }

    /*Обработчик нажатия мыши для окна "процент",
    * при нажатии показыватся диалог.
    * */
    private class MouseClickListener implements MouseListener {
        private Component frame;

        public MouseClickListener(Component parent) {
            this.frame = parent;
        }

        public void mouseClicked(MouseEvent e) {
            new RateDialog(frame, supplierRate);
        }

        public void mousePressed(MouseEvent e) {}

        public void mouseReleased(MouseEvent e) {}

        public void mouseEntered(MouseEvent e) {}

        public void mouseExited(MouseEvent e) {}
    }

    /*Диалог ввода процента поставщика.*/
    class RateDialog extends JDialog {
        JTextField textField;
        JButton buttonSave;

        public RateDialog(Component frame, Float initialRate) {
            setModal(true);
            setLayout(new BorderLayout());

            JLabel message = new JLabel("Enter supplier rate (%)");
            add(message, BorderLayout.NORTH);

            textField = new JTextField(20);
            textField.setPreferredSize(new Dimension(textField.getPreferredSize().width, 30));
            textField.setAlignmentX(JLabel.CENTER);
            add(textField);

            buttonSave = new JButton("Save");
            buttonSave.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    if (textField.getText().isEmpty()) return;
                    try{
                        Float rate = Float.parseFloat(textField.getText());
                        onDialogButtonClick(rate);
                    }
                    catch (NumberFormatException e){
                        return;
                    }
                    dispose();
                }
            });
            add(buttonSave, BorderLayout.SOUTH);

            if (initialRate >= 0){
                textField.setText(initialRate.toString());
            }

            pack();
            setLocation(frame.getLocation().x + frame.getWidth() / 2 - this.getWidth() / 2,
                    frame.getLocation().y + frame.getHeight() / 2 - this.getHeight() / 2);
            setVisible(true);
        }
    }

    private void onDialogButtonClick(Float input){
        textRate.setText(input.toString());
        supplierRate = input;
        computePriceCost();
    }

}
