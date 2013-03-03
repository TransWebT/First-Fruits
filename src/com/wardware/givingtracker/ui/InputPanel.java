package com.wardware.givingtracker.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyVetoException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.text.TextAction;

import com.michaelbaranov.microba.calendar.DatePicker;
import com.wardware.givingtracker.GivingRecord;
import com.wardware.givingtracker.RecordManager;
import com.wardware.givingtracker.Settings;

public class InputPanel extends JPanel implements Observer
{
    private static final SimpleDateFormat SDF = new SimpleDateFormat("MM/dd/yyyy");
    private AutoComboBox lastNameCombo;
    private AutoComboBox firstNameCombo;
    private List<CategoryInputPair> categoryInputs;
    private DatePicker picker;
    private NumberFormat simpleCurrencyFormat;
    private MyKeyListener keyListener;
    private JPanel contentPanel;

    public InputPanel()
    {
        initComponents();
        RecordManager.getInstance().addObserver(this);
        Settings.getInstance().addObserver(this);
    }

    private void initComponents()
    {
        setLayout(new BorderLayout());
        removeAll();
        
        contentPanel = new JPanel(new GridBagLayout());
        final JLabel dateLabel = new JLabel("Date");
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        contentPanel.add(dateLabel, Gbc.xyi(0, 0, 2).top(50).east());
        
        picker = new DatePicker(new Date());
        picker.setDateFormat(SDF);
        final String date = SDF.format(picker.getDate());
        RecordManager.getInstance().setSelectedDate(date);
        picker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                if (picker.getDate() != null) {
                    final String date = SDF.format(picker.getDate());
                    RecordManager.getInstance().setSelectedDate(date);
                    final GivingRecord selectedRecord = RecordManager.getInstance().getSelectedRecord();
                    if (selectedRecord != null) {
                        selectedRecord.setDate(date);
                        RecordManager.getInstance().setSelectedRecord(selectedRecord);
                    }
                }
            }
        });
        contentPanel.add(picker, Gbc.xyi(1, 0, 2).top(50).horizontal().right(5));
        
        final JLabel lastName = new JLabel("Last Name");
        lastName.setHorizontalAlignment(SwingConstants.RIGHT);
        contentPanel.add(lastName, Gbc.xyi(0, 2, 2).east().left(5));

        lastNameCombo = new AutoComboBox(new ArrayList<String>());
        lastNameCombo.setMinimumSize(new Dimension(150, lastNameCombo.getHeight()));
        lastNameCombo.setCaseSensitive(false);
        lastNameCombo.setStrict(false);
        lastNameCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (lastNameCombo.getSelectedItem() != null) {
                    final String lastNameSelection = lastNameCombo.getSelectedItem().toString();
                    final List<String> firstNames = new ArrayList<String>();
                    firstNames.add("");
                    firstNames.addAll(RecordManager.getInstance().getFirstNamesForLastName(lastNameSelection));
                    firstNameCombo.setDataList(firstNames);
                }
            }
        });
        contentPanel.add(lastNameCombo, Gbc.xyi(1, 2, 2).horizontal().right(5));
        
        final JLabel firstName = new JLabel("First Name");
        firstName.setHorizontalAlignment(SwingConstants.RIGHT);
        contentPanel.add(firstName, Gbc.xyi(0, 3, 2).east().left(5));

        firstNameCombo = new AutoComboBox(new ArrayList<String>());
        firstNameCombo.setMinimumSize(new Dimension(150, firstNameCombo.getHeight()));
        firstNameCombo.setCaseSensitive(false);
        firstNameCombo.setStrict(false);
        contentPanel.add(firstNameCombo, Gbc.xyi(1, 3, 2).horizontal().right(5));

        simpleCurrencyFormat = NumberFormat.getNumberInstance();
        simpleCurrencyFormat.setMaximumFractionDigits(2);
        simpleCurrencyFormat.setMinimumFractionDigits(2);

        keyListener = new MyKeyListener();

        categoryInputs = new ArrayList<CategoryInputPair>();
        int gridy = updateCategeries();
        
        final JButton add = new JButton(new TextAction("Add") {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                createOrUpdateRecord();
            }
        });
        contentPanel.add(add, Gbc.xyi(1, gridy++, 2).horizontal().right(5));
        add(contentPanel, BorderLayout.NORTH);
    }

    private class CategoryInputPair
    {
        private JLabel label;
        private JFormattedTextField inputField;

        public CategoryInputPair(JLabel label, JFormattedTextField inputField)
        {
            this.label = label;
            this.inputField = inputField;
        }

        public JLabel getLabel()
        {
            return label;
        }

        public JFormattedTextField getInputField()
        {
            return inputField;
        }
    }

    class MyKeyListener extends KeyAdapter
    {
        @Override
        public void keyReleased(KeyEvent event)
        {
            if (KeyEvent.VK_ENTER == event.getKeyCode()) {
                createOrUpdateRecord();
            }
            if (KeyEvent.VK_DOWN == event.getKeyCode()) {
                final CategoryInputPair[] inputs = new CategoryInputPair[categoryInputs.size()];
                categoryInputs.toArray(inputs);
                for (int i = 0; i < inputs.length; i++) {
                    if (inputs[i].getInputField().hasFocus()) {
                        if (i < inputs.length - 1) {
                            inputs[i+1].getInputField().requestFocusInWindow();
                        } else {
                            inputs[0].getInputField().requestFocusInWindow();
                        }
                        break;
                    }
                }
            }
            if (KeyEvent.VK_UP == event.getKeyCode()) {
                final CategoryInputPair[] inputs = new CategoryInputPair[categoryInputs.size()];
                categoryInputs.toArray(inputs);
                for (int i = 0; i < inputs.length; i++) {
                    if (inputs[i].getInputField().hasFocus()) {
                        if (i == 0) {
                            inputs[inputs.length-1].getInputField().requestFocusInWindow();
                        } else {
                            inputs[i-1].getInputField().requestFocusInWindow();
                        }
                        break;
                    }
                }
            }
        }
    }

    public void updateLastNames(List<String> names)
    {
        Collections.sort(names);
        lastNameCombo.setDataList(names);
        firstNameCombo.setDataList(new ArrayList<String>());
    }

    public int updateCategeries()
    {
        final List<String> categories = Settings.getInstance().getCategories();
        for (CategoryInputPair input : categoryInputs)
        {
            this.remove(input.getLabel());
            this.remove(input.getInputField());
        }
        contentPanel.invalidate();
        contentPanel.updateUI();
        categoryInputs.clear();

        int gridy = 4;
        for (String category : categories) {
            final JLabel categoryLabel = new JLabel(category);
            categoryLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            contentPanel.add(categoryLabel, Gbc.xyi(0, gridy, 2).left(5).east());

            final JFormattedTextField valueField = new CurrencyFormattedTextField();
            valueField.addKeyListener(keyListener);
            contentPanel.add(valueField, Gbc.xyi(1, gridy, 2).horizontal().right(5));

            categoryInputs.add(new CategoryInputPair(categoryLabel, valueField));
            gridy++;
        }
        invalidate();
        updateUI();
        return gridy;
    }

    private void createOrUpdateRecord()
    {
        if (lastNameCombo.getSelectedItem().toString().trim().isEmpty()) {
            return;
        }

        final GivingRecord record = new GivingRecord();
        record.setDate(SDF.format(picker.getDate()));
        record.setLastName(lastNameCombo.getSelectedItem().toString());
        record.setFirstName(firstNameCombo.getSelectedItem().toString());

        for (CategoryInputPair input : categoryInputs) {
            final String category = input.getLabel().getText();
            final String value = input.getInputField().getText();

            try {
                if (value != null && !value.isEmpty()) {
                    final Double amount = simpleCurrencyFormat.parse(value).doubleValue();
                    record.setAmountForCategory(category, amount);
                }
            } catch (ParseException e) {
                return;
            }
        }
        RecordManager.getInstance().updateRecord(record);
        lastNameCombo.requestFocusInWindow();
        lastNameCombo.setSelectedIndex(0);
        for (CategoryInputPair input : categoryInputs) {
            input.getInputField().setValue(null);
        }
    }

    @Override
    public void update(Observable o, Object value)
    {
        if (o instanceof RecordManager)
        {
            updateLastNames(RecordManager.getInstance().getUniqueLastNames());
            final GivingRecord selectedRecord = RecordManager.getInstance().getSelectedRecord();
            if (selectedRecord != null) {
                try {
                    picker.setDate(SDF.parse(selectedRecord.getDate()));
                } catch (PropertyVetoException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                lastNameCombo.setSelectedItem(selectedRecord.getLastName());
                firstNameCombo.setSelectedItem(selectedRecord.getFirstName());

                for (CategoryInputPair input : categoryInputs) {
                    final String category = input.getLabel().getText();
                    input.getInputField().setValue(selectedRecord.getAmountForCategory(category));
                }
            } else {
                lastNameCombo.setSelectedIndex(0);
                for (CategoryInputPair input : categoryInputs) {
                    input.getInputField().setValue(null);
                }
            }
        } else if (o instanceof Settings) {
            initComponents();
        }
    }
}
