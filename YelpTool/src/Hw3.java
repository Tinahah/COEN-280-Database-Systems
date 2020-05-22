
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author yanping kang
 */
public class Hw3 extends JFrame {

    private static Connection con;

    private HashSet<String> mainCgSet = new HashSet(),
            subCgSet = new HashSet(),
            attrSet = new HashSet();
    private String[] columnNames = {"NAME",
                        "BUSINESSID",
                        "ADDRESS",
                        "CITY",
                        "STATE",
                        "STARTS",
                        "REVIEWCOUNT",
                        "CHECKCOUNT"
                    };   
    DefaultTableModel tableModel_default;
    
    String bidFilter = "";
            
    public Hw3() {        
        System.out.println("Application initializing...");
        //GUI preparation.
        initComponents();
        System.out.println("Components initialized.");
        try {
            //Connect DB.
            con = DBConnection.getConnection();
            //Load initial data.
            tableModel_default = new DefaultTableModel();
            for (String colname : columnNames) {
                tableModel_default.addColumn(colname);
            }
            initMainCg();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /* Load main category data. */
    private void initMainCg() throws SQLException {
        JPanel MainCategoryPanel;
        MainCategoryPanel = (JPanel)MainCategoryJPanel.getViewport().getView();
        MainCategoryPanel.removeAll();
        MainCategoryPanel.setLayout(new GridLayout(0, 1));
        MainCategoryPanel.setBackground(Color.WHITE);
        String sql = "select distinct name from yelp_business_mainCatg order by name";
        System.out.println(sql);
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        int cnt = 0;
        while (rs.next()) {
            cnt++;
            JCheckBox mainCgCB = new JCheckBox();
            String mainCgName = rs.getString(1);
            mainCgCB.setText(mainCgName);
            MainCategoryPanel.add(mainCgCB);
            mainCgCB.addActionListener((ActionEvent e) -> {                
                if (mainCgCB.isSelected()) {
                    mainCgSet.add(mainCgName);
                } else {
                    mainCgSet.remove(mainCgName);
                }
                reloadInfo();
            });
        }
        MainCategoryPanel.revalidate();
        MainCategoryPanel.repaint();
        System.out.println("return: " + cnt + " rows.");
    }
    
    private String setToStr(HashSet hs) {
        if (hs.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
            hs.stream().forEach((cg) -> {
                sb.append("\'").append(cg).append("\',");
            });
        return sb.substring(0, sb.length() - 1);
    }
    
    private String filterBidSQLbyCgAttr(){
        String sql = "";
        if (mainCgSet.size() > 0) {
            if (ORradio.isSelected()) {
                sql = " where businessID in (select distinct businessID from yelp_businessCategory where mainCategory in (" 
                    + setToStr(mainCgSet) + 
                    ")";
                if (subCgSet.size() > 0) {
                    sql += " and subCategory in (" + setToStr(subCgSet) + ")";
                }            
                sql += ")";
                if (attrSet.size() > 0) {
                    sql += " and businessID in ( " 
                            + "select distinct businessID from yelp_businessAttribute" 
                            + " where attrNameValue in (" + setToStr(attrSet) + "))";
                }
            } else {
                sql = "";
                String temp_sql = "";
                for (String mcg : mainCgSet) {
                    temp_sql += "and exists (select * from yelp_businessCategory where t.businessID = businessID and mainCategory = '" 
                            + mcg + "') "; 
                }
                sql += " where " + temp_sql.substring(4);  
                if (subCgSet.size() > 0) {
                    for (String scg : subCgSet) {
                        sql += "and exists (select * from yelp_businessCategory where t.businessID = businessID and subCategory = '" 
                                + scg + "') "; 
                    }
                }            
                if (attrSet.size() > 0) {
                    for (String attr : attrSet) {
                        sql += "and exists (select * from yelp_businessAttribute where t.businessID = businessID and attrNameValue = '" 
                                + attr + "') "; 
                    }
                }
            }
        }
        return sql;
    }
    
    private String filterBidSQLbyHours() {
        String dayofw = String.valueOf(dayComboBox.getSelectedItem());            
        String openh = String.valueOf(openHourComboBox.getSelectedItem());
        String closeh = String.valueOf(closeHourComboBox.getSelectedItem());
        String sql = "";
        if (dayofw.equals("ALL") && openh.equals("ALL") && closeh.equals("ALL")) {
            return sql;
        }
        String filterDayofw = "";
        if (!dayofw.equals("ALL")) {
            filterDayofw = " days = \'" + dayofw + "\' ";
        }
        String filterOpenh = "";
        if (!openh.equals("ALL")) {
            filterOpenh = " openTime = \'" + openh + "\' ";
        }
        String filterCloseh = "";
        if (!closeh.equals("ALL")) {
            filterCloseh = " closeTime = \'" + closeh + "\' ";
        }
        sql = "select distinct businessID from yelp_businessHours where " + filterDayofw;
        if (!filterOpenh.equals("")) {
            if (!dayofw.equals("ALL")) {
                sql += " and ";
            }
            sql += filterOpenh;
        }
        if (!filterCloseh.equals("")) {
            if (!filterCloseh.equals("") || !filterOpenh.equals("")) {
                sql += " and ";
            }
            sql += filterCloseh;
        }
        return sql;
    }
    
    private String filterBidSQLbyLoc(){
        String sql = "";
        String loc = String.valueOf(locationComboBox.getSelectedItem());            
            if (!loc.equals("ALL")) {
                sql = " and businessID in (select distinct businessID from yelp_business ";
                String[] addr = loc.split(",");
                sql += "where city = \'" + addr[0] + "\' and state = \'" + addr[1] + "') ";  
            }
        return sql;
    }
    
    
    /* Load subcategory data. */
    private void updateSubCg() throws SQLException {
        subCgSet.clear();
        JPanel SubCategoryPanel;
        SubCategoryPanel = (JPanel)SubCategoryJPanel.getViewport().getView();
        SubCategoryPanel.removeAll();
        SubCategoryPanel.setLayout(new GridLayout(0, 1));
        SubCategoryPanel.setBackground(Color.WHITE);
        if (mainCgSet.size() > 0) {
            String sql = "select distinct subCategory from yelp_businessCategory c";
            if (ORradio.isSelected()) {
                sql += " where mainCategory in (" + setToStr(mainCgSet) + ") ";
            } else {
                String sql_and = "";
                for (String mcg : mainCgSet) {
                    sql_and += "and exists (select * from yelp_businessCategory where c.businessID = businessID and mainCategory = '" 
                            + mcg + "') "; 
                }
                sql += " where " + sql_and.substring(4);        
            }
            sql += "order by subCategory";
            System.out.println(sql);
            
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            int cnt = 0;
            while (rs.next()) {
                cnt++;
                JCheckBox subCgCB = new JCheckBox();
                String subCgName = rs.getString(1);
                subCgCB.setText(subCgName);
                SubCategoryPanel.add(subCgCB);
                subCgCB.addActionListener((ActionEvent e) -> {                    
                    if (subCgCB.isSelected()) {
                        subCgSet.add(subCgName);
                    } else {
                        subCgSet.remove(subCgName);
                    }
                    try {
                        clearBusiness();
                        
                        bidFilter = filterBidSQLbyCgAttr();
                        
                        //load attribute.
                        updateAttr();
                        
                        //load day of week.
                        updateDayofW();
                        
                        //load open hour.
                        updateOpenHour();
                        
                        //load close hour.
                        updateCloseHour();
                        
                        //load location.
                        updateLocation();
                    } catch (SQLException ex) {
                        Logger.getLogger(Hw3.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            } 
            System.out.println("return: " + cnt + " rows.");
        }
        SubCategoryPanel.revalidate();
        SubCategoryPanel.repaint();
    }
    
    /* Load attribute data. */
    private void updateAttr() throws SQLException {
        attrSet.clear();
        JPanel AttributesPanel;
        AttributesPanel = (JPanel)AttributesJPanel.getViewport().getView();
        AttributesPanel.removeAll();
        AttributesPanel.setLayout(new GridLayout(0, 1));  
        AttributesPanel.setBackground(Color.WHITE);
        if (mainCgSet.size() > 0) {
            String sql = "select distinct attrNameValue from yelp_businessAttribute t" 
                    + bidFilter + " order by attrNameValue";
            System.out.println(sql);

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            int cnt = 0;
            while (rs.next()) {
                    cnt++;
                    JCheckBox attrCB = new JCheckBox();
                    String attrName = rs.getString(1);
                    attrCB.setText(attrName);
                    AttributesPanel.add(attrCB);
                    attrCB.addActionListener((ActionEvent e) -> {                    
                        if (attrCB.isSelected()) {
                            attrSet.add(attrName);
                        } else {
                            attrSet.remove(attrName);
                        }
                        try {
                            clearBusiness();
                            
                            bidFilter = filterBidSQLbyCgAttr();
                            //load day of week.
                            updateDayofW();

                            //load open hour.
                            updateOpenHour();

                            //load close hour.
                            updateCloseHour();

                            //load location.
                            updateLocation();
                        } catch (SQLException ex) {
                            Logger.getLogger(Hw3.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
                } 
            System.out.println("return: " + cnt + " rows.");
        }
        AttributesPanel.revalidate();
        AttributesPanel.repaint();
    }
    
            /* Load location data. */
    private void updateLocation() throws SQLException {
        locationComboBox.removeAll();
        ArrayList<String> locationOptions = new ArrayList();
        locationOptions.add("ALL");
        if (mainCgSet.size() > 0) {
            String sql;
            sql = "select distinct city || ',' || state as loc from yelp_business t" 
                    + bidFilter + " order by loc";
            System.out.println(sql);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            int cnt = 0;
            while (rs.next()) {
                cnt++;
                locationOptions.add(rs.getString(1));

            } 
            System.out.println("return: " + cnt + " rows.");
        }

        locationComboBox.setModel(new DefaultComboBoxModel(locationOptions.toArray()));
        locationComboBox.revalidate();
        locationComboBox.repaint();
    }
    
    /* Load day of week data. */
    private void updateDayofW() throws SQLException {
        dayComboBox.removeAll();
        ArrayList<String> dayOptions = new ArrayList();
        dayOptions.add("ALL");          
        if (mainCgSet.size() > 0) {
            String sql = "select distinct days from yelp_businessHours t" 
                + bidFilter + filterBidSQLbyLoc() + " order by days";
            System.out.println(sql);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            int cnt = 0;
            while (rs.next()) {
                cnt++;
                dayOptions.add(rs.getString(1));

            } 
            System.out.println("return: " + cnt + " rows.");
        }
        dayComboBox.setModel(new DefaultComboBoxModel(dayOptions.toArray()));
        dayComboBox.revalidate();
        dayComboBox.repaint();
    }
    
            
    private String filterByDay() {
        String sql = "";
        String dayofw = String.valueOf(dayComboBox.getSelectedItem());            
            if (!dayofw.equals("ALL")) {
                sql += " and days = \'" + dayofw + "\' ";
            }
        return sql;
    }
    
    /* Load open hour data. */
    private void updateOpenHour() throws SQLException {
        openHourComboBox.removeAll();
        ArrayList<String> openHourOptions = new ArrayList();
        openHourOptions.add("ALL");
        if (mainCgSet.size() > 0) {
            String sql = "select distinct openTime from yelp_businessHours t" 
                    + bidFilter + filterBidSQLbyLoc() + filterByDay() + " order by openTime";
            System.out.println(sql);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            int cnt = 0;
            while (rs.next()) {
                cnt++;
                openHourOptions.add(rs.getString(1));

            } 
            System.out.println("return: " + cnt + " rows.");
        }

        openHourComboBox.setModel(new DefaultComboBoxModel(openHourOptions.toArray()));
        openHourComboBox.revalidate();
        openHourComboBox.repaint();
    }
    
    /* Load close time data. */
    private void updateCloseHour() throws SQLException {
        closeHourComboBox.removeAll();
        ArrayList<String> closeHourOptions = new ArrayList();
        closeHourOptions.add("ALL");   
        if (mainCgSet.size() > 0) {
            String sql = "select distinct closeTime from yelp_businessHours t" 
                    + bidFilter + filterBidSQLbyLoc() + filterByDay() + " order by closeTime";
            System.out.println(sql);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            int cnt = 0;
            while (rs.next()) {
                cnt++;
                closeHourOptions.add(rs.getString(1));

            } 
            System.out.println("return: " + cnt + " rows.");
        }
        
        closeHourComboBox.setModel(new DefaultComboBoxModel(closeHourOptions.toArray()));
        closeHourComboBox.revalidate();
        closeHourComboBox.repaint();
    }
    
    public void resultSetToTableModel(ResultSet rs, JTable t) throws SQLException{
        //Create new table model
        DefaultTableModel tableModel = new DefaultTableModel();

        //Retrieve meta data from ResultSet
        ResultSetMetaData metaData = rs.getMetaData();

        //Get number of columns from meta data
        int columnCount = metaData.getColumnCount();

        //Get all column names from meta data and add columns to table model
        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++){
            tableModel.addColumn(metaData.getColumnLabel(columnIndex));
        }

        //Create array of Objects with size of column count from meta data
        Object[] row = new Object[columnCount];
        int cnt = 0;
        
        //Scroll through result set
        while (rs.next()){
            cnt++;
            //Get object from column with specific index of result set to array of objects
            for (int i = 0; i < columnCount; i++){
                row[i] = rs.getObject(i+1);
            }
            //Now add row to table model with that array of objects as an argument
            tableModel.addRow(row);
        }
        System.out.println("return: " + cnt + " rows.");

        //Now add that table model to your table and you are done :D
        t.setModel(tableModel);
    }
    
    /* Load business data. */
    private void getBusinessResult() throws SQLException {
        if (mainCgSet.size() > 0) {          
            String sql = "select * from v_yelp_business_info t" + filterBidSQLbyCgAttr(); 
            if (!filterBidSQLbyHours().equals("")) {
                sql += " and businessID in (" + filterBidSQLbyHours() + ") ";
            } 
            String loc = String.valueOf(locationComboBox.getSelectedItem());            
            if (!loc.equals("ALL")) {
                String[] addr = loc.split(",");
                sql += " and city = \'" + addr[0] + "\' and state = \'" + addr[1] + "'";  
            }
            
            sql +=  " order by name";
            System.out.println(sql);
            
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            resultSetToTableModel(rs, table);
            //set address column width.
            table.getColumnModel().getColumn(2).setPreferredWidth(150);
        }
        table.revalidate();
    }
    
    private void showReview(String bid, String bname) throws SQLException {  
        String sql = "select r.rdate as review_date, r.stars, to_char(substr(r.text, 1, 4000)) as text, u.name as user_name, r.votesCool as cool_votes, r.votesFunny as funny_votes, r.votesUseful as useful_votes\n" +
"from (select * from yelp_review where businessID = '" + bid + "') r\n" +
"left join\n" +
"yelp_yelpUser u\n" +
"on r.user_id = u.yelpID";
        System.out.println(sql);

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        JTable reviewTable = new JTable();
        resultSetToTableModel(rs, reviewTable);
        //set text column width.
        reviewTable.getColumnModel().getColumn(2).setPreferredWidth(250);

        JFrame reviewFrame = new JFrame("User Reviews for Business \"" + bname + "\"");
        reviewFrame.setLocation(500,200);
        reviewFrame.setSize(900,500);
        reviewTable.setDefaultEditor(Object.class, null);
        JScrollPane jpane = new JScrollPane(reviewTable);
        jpane.setAutoscrolls(true);
        reviewFrame.add(jpane);
        reviewFrame.setVisible(true);
    }
    
    private void clearBusiness() {
        //clear business result.      
        table.setModel(tableModel_default);
        BusinessJPanel.setViewportView(table);
    }
    
    private void reloadInfo() {
        try {
            
            clearBusiness();
            
            bidFilter = filterBidSQLbyCgAttr();
            
            //load subcategory.
            updateSubCg();
            
            //load attribute.
            updateAttr();
            
            //load day of week.
            updateDayofW();
            
            //load open hour.
            updateOpenHour();
            
            //load close hour.
            updateCloseHour();
            
            //load location.
            updateLocation();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    /* GUI preparation. */
    // <editor-fold defaultstate="collapsed" desc="init Components">                          
    private void initComponents() {

        MainCategoryJPanel = new JScrollPane(new JPanel());
        SubCategoryJPanel = new JScrollPane(new JPanel());
        AttributesJPanel = new JScrollPane(new JPanel());
        
        String data[][] = {};
        table = new JTable(data, columnNames); 
        table.setDefaultEditor(Object.class, null);
        BusinessJPanel = new JScrollPane(table);
        table.setFillsViewportHeight(true);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                JTable source = (JTable)evt.getSource();
                int row = source.rowAtPoint( evt.getPoint() );
                if (evt.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    String bid = (String) source.getModel().getValueAt(row, 1);
                    String bname = (String) source.getModel().getValueAt(row, 0);
                    try {
                        showReview(bid, bname);
                    } catch (SQLException ex) {
                        Logger.getLogger(Hw3.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        
        RelationPanel = new JPanel();
        relation = new ButtonGroup();
        ANDradio = new JRadioButton();
        ORradio = new JRadioButton();
        ORradio.setSelected(true);
        relation.add(ANDradio);
        relation.add(ORradio);
        ANDradio.addActionListener((ActionEvent e) -> {
            subCgSet.clear();
            attrSet.clear();
            reloadInfo();
        });
        
        ORradio.addActionListener((ActionEvent e) -> {
            subCgSet.clear();
            attrSet.clear();
            reloadInfo();
        });
        
        DayofWeekPanel = new JPanel();
        dayComboBox = new JComboBox();
        OpenHourPanel = new JPanel();
        openHourComboBox = new JComboBox();
        CloseHourPanel = new JPanel();
        closeHourComboBox = new JComboBox();
        LocationPanel = new JPanel();
        locationComboBox = new JComboBox();
        searchButton = new JButton();
        clearButton = new JButton();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        MainCategoryJPanel.setViewportBorder(BorderFactory.createTitledBorder(BorderFactory.createTitledBorder("MainCategory")));
        MainCategoryJPanel.setPreferredSize(new Dimension(245, 800));
        MainCategoryJPanel.setAutoscrolls(true);
        
        SubCategoryJPanel.setViewportBorder(BorderFactory.createTitledBorder("SubCategory"));
        SubCategoryJPanel.setPreferredSize(new Dimension(245, 800));
        SubCategoryJPanel.setAutoscrolls(true);
        
        AttributesJPanel.setViewportBorder(BorderFactory.createTitledBorder("Attributes"));
        AttributesJPanel.setPreferredSize(new Dimension(245, 800));
        AttributesJPanel.setAutoscrolls(true);
        
        BusinessJPanel.setBorder(BorderFactory.createTitledBorder("Business Search Result"));
        BusinessJPanel.setPreferredSize(new Dimension(1000, 800));
        BusinessJPanel.setAutoscrolls(true);

        RelationPanel.setBorder(BorderFactory.createTitledBorder("Relation"));

        ANDradio.setText("AND");
        ORradio.setText("OR");

        GroupLayout RelationPanelLayout = new GroupLayout(RelationPanel);
        RelationPanel.setLayout(RelationPanelLayout);
        RelationPanelLayout.setHorizontalGroup(
                RelationPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(RelationPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(ANDradio, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                        .addComponent(ORradio, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
        );
        RelationPanelLayout.setVerticalGroup(
                RelationPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(RelationPanelLayout.createSequentialGroup()
                        .addGroup(RelationPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(ANDradio)
                                .addComponent(ORradio, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE))
        );

        DayofWeekPanel.setBorder(BorderFactory.createTitledBorder("Day of Week"));

        dayComboBox.setModel(new DefaultComboBoxModel(new String[]{"ALL"}));

        GroupLayout DayofWeekLayout = new GroupLayout(DayofWeekPanel);
        DayofWeekPanel.setLayout(DayofWeekLayout);
        DayofWeekLayout.setHorizontalGroup(
                DayofWeekLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(dayComboBox, 0, 233, Short.MAX_VALUE)
        );
        DayofWeekLayout.setVerticalGroup(
                DayofWeekLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, DayofWeekLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(dayComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );
        
        dayComboBox.addActionListener ((ActionEvent e) -> {
            try {
                updateOpenHour();
                updateCloseHour();
            } catch (SQLException ex) {
                Logger.getLogger(Hw3.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        OpenHourPanel.setBorder(BorderFactory.createTitledBorder("Open Hour"));

        openHourComboBox.setModel(new DefaultComboBoxModel(new String[]{"ALL"}));

        GroupLayout OpenHourLayout = new GroupLayout(OpenHourPanel);
        OpenHourPanel.setLayout(OpenHourLayout);
        OpenHourLayout.setHorizontalGroup(
                OpenHourLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(openHourComboBox, 0, 188, Short.MAX_VALUE)
        );
        OpenHourLayout.setVerticalGroup(
                OpenHourLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, OpenHourLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(openHourComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        CloseHourPanel.setBorder(BorderFactory.createTitledBorder("Close Hour"));

        closeHourComboBox.setModel(new DefaultComboBoxModel(new String[]{"ALL"}));

        GroupLayout CloseHourLayout = new GroupLayout(CloseHourPanel);
        CloseHourPanel.setLayout(CloseHourLayout);
        CloseHourLayout.setHorizontalGroup(
                CloseHourLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(closeHourComboBox, 0, 188, Short.MAX_VALUE)
        );
        CloseHourLayout.setVerticalGroup(
                CloseHourLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, CloseHourLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(closeHourComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        LocationPanel.setBorder(BorderFactory.createTitledBorder("Location"));

        locationComboBox.setModel(new DefaultComboBoxModel(new String[]{"ALL"}));
        
        locationComboBox.addActionListener ((ActionEvent e) -> {
            try {
                updateDayofW();
                updateOpenHour();
                updateCloseHour();
            } catch (SQLException ex) {
                Logger.getLogger(Hw3.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        GroupLayout LocationLayout = new GroupLayout(LocationPanel);
        LocationPanel.setLayout(LocationLayout);
        LocationLayout.setHorizontalGroup(
                LocationLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(locationComboBox, 0, 233, Short.MAX_VALUE)
        );
        LocationLayout.setVerticalGroup(
                LocationLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, LocationLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(locationComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        searchButton.setBackground(new Color(0, 102, 153));
        searchButton.setForeground(new Color(255, 255, 255));
        searchButton.setText("Search");
        searchButton.setFont(new Font("Courier", Font.BOLD, 16));
        searchButton.setPreferredSize(new Dimension(150, 50));
        searchButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (mainCgSet.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please select a main category first!");
                } else {
                    try {
                        getBusinessResult();
                    } catch (SQLException ex) {
                        Logger.getLogger(Hw3.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        clearButton.setBackground(new Color(0, 102, 153));
        clearButton.setForeground(new Color(255, 255, 255));
        clearButton.setText("Clear");
        clearButton.setFont(new Font("Arial", Font.BOLD, 16));
        clearButton.setPreferredSize(new Dimension(80, 50));
        clearButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                SubCategoryJPanel.setViewportView(new JPanel());        
                subCgSet.clear();
                AttributesJPanel.setViewportView(new JPanel());       
                attrSet.clear();
                try {
                    mainCgSet.clear();
                    
                    //load day of week.
                    updateDayofW();

                    //load open hour.
                    updateOpenHour();

                    //load close hour.
                    updateCloseHour();

                    //load location.
                    updateLocation();
                    
                    //clear business result.
                    clearBusiness();
                    
                    //load main category data.
                    initMainCg();
                } catch (Exception ex) {
                    Logger.getLogger(Hw3.class.getName()).log(Level.SEVERE, null, ex);
                }

                
                
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(MainCategoryJPanel)
                                .addComponent(RelationPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(LocationPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(SubCategoryJPanel))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(DayofWeekPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(OpenHourPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(CloseHourPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(searchButton, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(clearButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(AttributesJPanel, GroupLayout.PREFERRED_SIZE, 284, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(BusinessJPanel, GroupLayout.PREFERRED_SIZE, 900, GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(MainCategoryJPanel)
                                .addComponent(AttributesJPanel)
                                .addComponent(SubCategoryJPanel)
                                .addComponent(BusinessJPanel, GroupLayout.DEFAULT_SIZE, 729, Short.MAX_VALUE))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addComponent(LocationPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(RelationPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(DayofWeekPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(OpenHourPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(CloseHourPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(searchButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(clearButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(26, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>                        

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Hw3.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        //</editor-fold>

        /* Create and display the form */
        EventQueue.invokeLater(() -> {
            JFrame jf = new Hw3();
            jf.setTitle("Yelp Query Application");
            jf.setVisible(true);
        });
    }

    // Variables declaration
    private JScrollPane MainCategoryJPanel;
    private JScrollPane SubCategoryJPanel;
    private JScrollPane AttributesJPanel;
    private JScrollPane BusinessJPanel;
    private JTable table;
    private JPanel RelationPanel;
    private ButtonGroup relation;
    private JRadioButton ANDradio, ORradio;
    private JPanel LocationPanel, DayofWeekPanel, OpenHourPanel, CloseHourPanel;
    private JComboBox dayComboBox, openHourComboBox, closeHourComboBox, locationComboBox;
    private JButton searchButton, clearButton;
    // End of variables declaration   
}
