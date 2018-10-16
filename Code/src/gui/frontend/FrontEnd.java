package gui.frontend;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * @author santi
 */
public class FrontEnd extends JPanel {
    
    private FrontEnd() throws Exception {
        super(new GridLayout(1, 1));
         
        JTabbedPane tabbedPane = new JTabbedPane();
         
        FEStatePane panel1 = new FEStatePane();
        tabbedPane.addTab("States", null, panel1, "Load/save states and play games.");
         
        JComponent panel2 = new FETracePane(panel1);
        tabbedPane.addTab("Traces", null, panel2, "Load/save and view replays.");
        
        JComponent panel3 = new FETournamentPane();
        tabbedPane.addTab("Tournaments", null, panel3, "Run tournaments.");

        //Add the tabbed pane to this panel.
        add(tabbedPane);
         
        //The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);   
    }
    
    public static void main(String args[]) throws Exception {
        JFrame frame = new JFrame("microRTS Front End");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);         
        frame.add(new FrontEnd(), BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }    
}
