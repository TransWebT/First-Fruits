package us.wardware.firstfruits.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class NumbersOnlyFilter extends DocumentFilter
{
    public void insertString(DocumentFilter.FilterBypass fb, int offset,
                    String text, AttributeSet attr) throws BadLocationException
    {
        final String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
        final StringBuilder sb = new StringBuilder(currentText);
        sb.insert(offset, text);
        {
            final Pattern p = Pattern.compile("^[0-9]{0,}$");
            final Matcher m = p.matcher(sb.toString());
            if (m.matches()) {
                fb.insertString(offset, text, attr);
            }
        }
    }

    // no need to override remove(): inherited version allows all removals

    public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
                    String text, AttributeSet attr) throws BadLocationException
    {
        final String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
        final StringBuilder sb = new StringBuilder(currentText);
        sb.replace(offset, offset + length, text);
        final Pattern p = Pattern.compile("^[0-9]{0,}$");
        final Matcher m = p.matcher(sb.toString());
        if (m.matches()) {
            fb.replace(offset, length, text, attr);
        }
    }

    public static void main(String[] args)
    {
        final DocumentFilter dfilter = new NumbersOnlyFilter();

        final JTextField jtf = new JTextField();
        ((AbstractDocument) jtf.getDocument()).setDocumentFilter(dfilter);

        final JFrame frame = new JFrame("NumbersOnlyFilter");
        frame.getContentPane().add(jtf, java.awt.BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(240, 120);
        frame.setVisible(true);
    }
}
