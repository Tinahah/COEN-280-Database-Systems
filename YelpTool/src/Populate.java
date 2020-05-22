
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author yanping kang
 */
public class Populate {

    private static Connection con;
    
    private static final String businessFilePath = System.getProperty("user.dir") + "\\src\\Dataset\\yelp_business.json";
    private static final String checkinFilePath = System.getProperty("user.dir") + "\\src\\Dataset\\yelp_checkin.json";
    private static final String reviewFilePath = System.getProperty("user.dir") + "\\src\\Dataset\\yelp_review.json";    
    private static final String userFilePath = System.getProperty("user.dir") + "\\src\\Dataset\\yelp_user.json";
    
    private static final String[] mainCategoryArr = new String[]{"Active Life", "Arts & Entertainment",
        "Automotive", "Car Rental", "Cafes", "Beauty & Spas", "Convenience Stores", "Dentists", "Doctors",
        "Drugstores", "Department Stores", "Education", "Event Planning & Services", "Flowers & Gifts", "Food",
        "Health & Medical", "Home Services", "Home & Garden", "Hospitals", "Hotels & Travel", "Hardware Stores",
        "Grocery", "Medical Centers", "Nurseries & Gardening", "Nightlife", "Restaurants", "Shopping", "Transportation"};

    //To prevent redundancy, delete existing business data.
    public static void businessDataDel() {
        try {
            Statement st;
            st = con.createStatement();
            //delete existing data of table yelp_business, yelp_business_mainCatg,
            // yelp_businessSubCategory, yelp_businessHours, yelp_businessAttribute.
            int cnt = st.executeUpdate("delete from yelp_business_mainCatg");
            System.out.println(cnt + " rows of main category deleted.");
            
            cnt = st.executeUpdate("delete from yelp_businessCategory");
            System.out.println(cnt + " rows of businessCategory deleted.");
            
            cnt = st.executeUpdate("delete from yelp_businessHours");
            System.out.println(cnt + " rows of businessHours deleted.");
            
            cnt = st.executeUpdate("delete from yelp_businessAttribute");
            System.out.println(cnt + " rows of businessAttribute deleted.");
            
            cnt = st.executeUpdate("delete from yelp_business");
            System.out.println(cnt + " rows of business deleted.");
            st.close();
        } catch(SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    //Populate table yelp_business_mainCatg.
    public static void businessMainCategoryIns() {
        try {
            String sql_mc = "insert into yelp_business_mainCatg values(?)";
            PreparedStatement ps_mc;
            ps_mc = con.prepareStatement(sql_mc);
            int cnt = 0;
            for (String mc : mainCategoryArr) {
                ps_mc.setString(1, mc);
                ps_mc.executeUpdate();
                cnt++;
            }
            System.out.println(cnt + " business main categories are stored!");
            ps_mc.close();
        } catch(SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    //Parse yelp_business.json and populate related business tables.
    public static void businessDataIns() {
        try {
            //populate main category.
            businessMainCategoryIns();

            /* populate businees and business subcategory/hours/attributes table. */      
            // business preparedStatement.
            String sql_business = "insert into yelp_business values(?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps_business;
            ps_business = con.prepareStatement(sql_business);

            // subcategory preparedStatement.
            String sql_cg = "insert into yelp_businessCategory values(?,?,?)";
            PreparedStatement ps_cg;
            ps_cg = con.prepareStatement(sql_cg);

            // hours preparedStatement.
            String sql_hours = "insert into yelp_businessHours values(?,?,?,?)";
            PreparedStatement ps_hours;
            ps_hours = con.prepareStatement(sql_hours);

            // attributes preparedStatement.
            String sql_attr = "insert into yelp_businessAttribute values(?,?)";
            PreparedStatement ps_attr;
            ps_attr = con.prepareStatement(sql_attr);

            /* Parse business json file and populate tables. */
            // FileReader reads files in the default encoding.
            FileReader fileReader = new FileReader(businessFilePath);
            System.out.println("(about 20 minutes) data processing ...");
            //parse and insert business info line by line.
            HashSet<String> mainCategorySet = new HashSet<>(Arrays.asList(mainCategoryArr));
            try ( // Always wrap FileReader in BufferedReader.
                    BufferedReader br = new BufferedReader(fileReader)) {
                int count = 0;
                String line;
                 while ((line = br.readLine()) != null) {
                    JSONObject obj;
                    obj = (JSONObject) new JSONParser().parse(line);

                    //join neighbors with ",".
                    String neighbors = "";
                    JSONArray list = (JSONArray) obj.get("neighborhoods");
                    Iterator<String> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        neighbors += iterator.next() + ",";
                    }
                    //business data. 20544 rows, about 5 minutes.
                    String business_id = (String) obj.get("business_id");
                    ps_business.setString(1, business_id);
                    ps_business.setString(2, (String) obj.get("full_address"));
                    ps_business.setBoolean(3, (Boolean) obj.get("open"));
                    ps_business.setString(4, (String) obj.get("city"));
                    ps_business.setString(5, (String) obj.get("state"));
                    ps_business.setDouble(6, (Double) obj.get("latitude"));
                    ps_business.setDouble(7, (Double) obj.get("longitude"));
                    ps_business.setLong(8, (long) obj.get("review_count"));
                    ps_business.setString(9, (String) obj.get("name"));
                    ps_business.setString(10, neighbors);
                    ps_business.setDouble(11, (double) obj.get("stars"));

                    ps_business.executeUpdate();
                    count++;

                    //business category data.
                    ps_cg.setString(1, business_id);
                    ArrayList<String> mainCategories = new ArrayList(), subCategories = new ArrayList();
                    JSONArray lst = (JSONArray) obj.get("categories");
                    Iterator<String> itr = lst.iterator();
                    while (itr.hasNext()) {
                        String cg = itr.next();
                        if (mainCategorySet.contains(cg)) {
                            mainCategories.add(cg);
                        } else {
                            subCategories.add(cg);
                        }
                    }
                    for (String maincg : mainCategories) {
                        for (String subcg : subCategories) {
                            ps_cg.setString(2, maincg);
                            ps_cg.setString(3, subcg);
                            ps_cg.executeUpdate();
                        }
                    }

                    //business hours data.
                    ps_hours.setString(1, business_id);
                    JSONObject hours;
                    hours = (JSONObject) obj.get("hours");
                    hours.keySet().forEach(key -> {
                        try {
                            ps_hours.setString(2, (String) key);
                            JSONObject timeObj = (JSONObject) hours.get(key);
                            String closeTime = (String) timeObj.get("close");
                            String openTime = (String) timeObj.get("open");
                            ps_hours.setString(3, closeTime);
                            ps_hours.setString(4, openTime);

                            ps_hours.executeUpdate();

                        } catch (SQLException ex) {
                            System.out.println(ex.getMessage());
                        }
                    });

                    //business attribute data.
                    ps_attr.setString(1, business_id);
                    JSONObject attributes;
                    attributes = (JSONObject) obj.get("attributes");
                    attributes.keySet().forEach(key -> {
                        try {
                            Class cls = attributes.get(key).getClass();
                            if (cls.toString().equals("class org.json.simple.JSONObject")) {
                                JSONObject attrs;
                                attrs = (JSONObject) attributes.get(key);
                                attrs.keySet().forEach(k -> {
                                    try {
                                        String attrValue = String.valueOf((Object)attrs.get(k));
                                        ps_attr.setString(2, (String) key + "_" + k + "_" + attrValue);
                                        ps_attr.executeUpdate();
                                    } catch (SQLException ex) {
                                        System.out.println(ex.getMessage()); 
                                    }
                                });                                
                            } else {
                                String attrValue = String.valueOf((Object)attributes.get(key));
                                ps_attr.setString(2, (String) key + "_" + attrValue);
                                ps_attr.executeUpdate();
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });
                }
                // Always close files.
                br.close();
                System.out.println(count + " rows of business data are inserted!");
            }
            ps_business.close();
            ps_cg.close();
            ps_hours.close();
            ps_attr.close();
        } catch(SQLException | IOException | ParseException ex) {
            ex.printStackTrace();
        }
    }
    
    //To prevent redundancy, delete existing review data.
    public static void reviewDataDel() {
        try {
            Statement st;
            st = con.createStatement();
            int cnt = st.executeUpdate("delete from yelp_review");
            st.close();
            System.out.println("Existing " + cnt + " rows of review data deleted.");
        } catch(SQLException ex) {
            System.out.println(ex);
        }
    }

    //Parse yelp_review.json and populate review table.
    public static void reviewDataIns() {    
        try {
            // review preparedStatement.
            String sql = "insert into yelp_review values(?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps;
            ps = con.prepareStatement(sql);

            // FileReader reads files in the default encoding.
            FileReader fileReader = new FileReader(reviewFilePath);
            System.out.println("(about 20 minutes) data processing ...");
            //parse and insert data line by line.
            try ( // Always wrap FileReader in BufferedReader.
                    BufferedReader br = new BufferedReader(fileReader)) {
                int count = 0;
                String line;
                while ((line = br.readLine()) != null) {
                    JSONObject obj;
                    obj = (JSONObject) new JSONParser().parse(line);

                    JSONObject votes;
                    votes = (JSONObject) obj.get("votes");

                    //review data. 826190 rows, about 19 minutes.
                    ps.setString(1, (String) obj.get("review_id"));
                    ps.setLong(2, (long) votes.get("funny"));
                    ps.setLong(3, (long) votes.get("useful"));
                    ps.setLong(4, (long) votes.get("cool"));
                    ps.setLong(5, (long) obj.get("stars"));
                    ps.setString(6, (String) obj.get("date"));
                    ps.setString(7, (String) obj.get("text"));
                    ps.setString(8, (String) obj.get("user_id"));
                    ps.setString(9, (String) obj.get("business_id"));

                    ps.executeUpdate();
                    count++;
                }
                // Always close files.
                br.close();
                System.out.println(count + " rows of review data are inserted!");
            }
            //close statement.
            ps.close();
        } catch(SQLException | IOException | ParseException ex) {
            ex.printStackTrace();
        }
    }

    //To prevent redundancy, delete existing checkin data.
    public static void checkinDataDel() {
        try {
            Statement st;
            st = con.createStatement();
            int cnt = st.executeUpdate("delete from yelp_businessCheckin");
            st.close();
            System.out.println("Existing " + cnt + " rows of checkin data deleted.");
        } catch(SQLException ex) {
            System.out.println(ex);
        }
    }
    
    //function to parse yelp_checkin.json and populate checkin table.
    public static void checkinDataIns() {
        try {
            // checkin preparedStatement.
            String sql = "insert into yelp_businessCheckin values(?,?,?)";
            PreparedStatement ps;
            ps = con.prepareStatement(sql);

            // FileReader reads files in the default encoding.
            FileReader fileReader = new FileReader(checkinFilePath);
            System.out.println("(about 20 minutes) data processing ...");
            //parse and insert data line by line.
            try ( // Always wrap FileReader in BufferedReader.
                    BufferedReader br = new BufferedReader(fileReader)) {
                int count = 0;
                String line;
                while ((line = br.readLine()) != null) {
                    JSONObject obj;
                    obj = (JSONObject) new JSONParser().parse(line);
                    //checkin data. 16896 lines, about 10 minutes.
                    ps.setString(1, (String) obj.get("business_id"));
                    count++;

                    JSONObject checkin_info;
                    checkin_info = (JSONObject) obj.get("checkin_info");
                    checkin_info.keySet().forEach((Object key) -> {
                        try {
                            ps.setString(2, (String) key);
                            long num = (Long) checkin_info.get(key);
                            ps.setLong(3, num);

                            ps.executeUpdate();

                        } catch (SQLException ex) {
                            System.out.println(ex.getMessage()); 
                        }
                    });
                }
                // Always close files.
                br.close();
                System.out.println(count + " lines of business checkin data are inserted!");
            }
            //close statement.
            ps.close();
        } catch(SQLException | IOException | ParseException ex) {
            System.out.println(ex);
        }
    }

    //To prevent redundancy, delete existing user data.
    public static void yelpUserDataDel() {
        try {
            Statement st;
            st = con.createStatement();
            int cnt = st.executeUpdate("delete from yelp_yelpUser");
            st.close();
            System.out.println("Existing " + cnt + " rows of user data deleted.");
        } catch(SQLException ex) {
            System.out.println(ex);
        }
    }
    
    //Parse yelp_user.json and populate user related tables.
    public static void yelpUserDataIns() {
        try {
            //user preparedSatement.
            String sql = "insert into yelp_yelpUser values(?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps;
            ps = con.prepareStatement(sql);

            //compliments info of user preparedStatement.
            PreparedStatement ps_comp;
            ps_comp = con.prepareStatement("insert into yelp_yelpUserCompliment values(?,?,?)");

            // FileReader reads files in the default encoding.
            FileReader fileReader = new FileReader(userFilePath);
            System.out.println("(about 10 minutes) data processing ...");
            //parse and insert data line by line.
            try ( // Always wrap FileReader in BufferedReader.
                    BufferedReader br = new BufferedReader(fileReader)) {
                int count = 0;
                String line;
                while ((line = br.readLine()) != null) {
                    JSONObject obj;
                    obj = (JSONObject) new JSONParser().parse(line);
                    //user data. 211002 lines, about 10 minutes.

                    JSONObject votes;
                    votes = (JSONObject) obj.get("votes");

                    String user_id;
                    user_id = (String) obj.get("user_id");

                    ps.setString(1, user_id);
                    ps.setString(2, (String) obj.get("yelp_since"));
                    ps.setLong(3, (long) votes.get("funny"));
                    ps.setLong(4, (long) votes.get("cool"));
                    ps.setLong(5, (long) votes.get("useful"));
                    ps.setLong(6, (long) obj.get("review_count"));
                    ps.setString(7, (String) obj.get("name"));
                    ps.setDouble(8, (double) obj.get("average_stars"));

                    //join friends with ",". 
                    String friends = "";
                    JSONArray list = (JSONArray) obj.get("friends");
                    Iterator<String> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        String friend = iterator.next();
                        friends += friend + ",";
                    }
                    ps.setString(9, friends.length() > 0 ? friends.substring(0, friends.length() - 1) : "");
                    ps.setLong(10, (long) obj.get("fans"));

                    //join elite with ",". 
                    String elite = "";
                    JSONArray elist = (JSONArray) obj.get("elite");
                    Iterator<Long> eiterator;
                    eiterator = elist.iterator();
                    while (eiterator.hasNext()) {
                        long el = eiterator.next();
                        elite += el + ",";
                    }
                    ps.setString(11, elite.length() > 0 ? elite.substring(0, elite.length() - 1) : "");

                    ps.executeUpdate();
                    count++;

                    //parse and insert user compliments data.
                    ps_comp.setString(1, user_id);
                    JSONObject compliments;
                    compliments = (JSONObject) obj.get("compliments");
                    compliments.keySet().forEach((Object key) -> {
                        try {
                            ps_comp.setString(2, (String) key);

                            long compValue;
                            compValue = ((Long) compliments.get(key));
                            ps_comp.setLong(3, compValue);

                            ps_comp.executeUpdate();

                        } catch (SQLException ex) {
                            System.out.println(ex.getMessage()); 
                        }
                    });
                }
                // Always close files.
                br.close();
                System.out.println(count + " rows of user data are inserted!");
            }
            //close statement.
            ps.close();
        } catch(SQLException | IOException | ParseException ex) {
            System.out.println(ex);
        }
    }

    //Function to read json files and populate DB.
    public static void populateDB(String[] args) throws SQLException, IOException, ParseException {
        //Connect DB.
        con = DBConnection.getConnection();
        /* //parse json files. */
        //To prevent integrity constraint, parse file in the order of business, user, review, checkin.
        List<String> list = Arrays.asList(args);
        List<String> orderedList = new ArrayList();
        if (list.contains("yelp_business.json")) {
            orderedList.add("yelp_business.json");
        }
        if (list.contains("yelp_user.json")) {
            orderedList.add("yelp_user.json");
        }
        if (list.contains("yelp_review.json")) {
            orderedList.add("yelp_review.json");
        }
        if (list.contains("yelp_checkin.json")) {
            orderedList.add("yelp_checkin.json");
        }

        for (String fileName : orderedList) {
            switch (fileName) {
                case "yelp_business.json":
                    System.out.println("Start yelp_business.json populating...");
                    businessDataDel();
                    businessDataIns();
                    break;
                case "yelp_review.json":
                    System.out.println("Start yelp_review.json populating...");
                    reviewDataDel();
                    reviewDataIns();
                    break;
                case "yelp_checkin.json":
                    System.out.println("Start yelp_checkin.json populating....");
                    checkinDataDel();
                    checkinDataIns();
                    break;
                case "yelp_user.json":
                    System.out.println("Start yelp_user.json populating....");
                    yelpUserDataDel();
                    yelpUserDataIns();
                    break;
            }
        }
        //Close DB.
        DBConnection.closeConnection(con);
    }

    public static void main(String[] args) {
        try {
            populateDB(args);
        } catch (SQLException | IOException | ParseException ex) {
            System.out.println(ex.getMessage()); 
        }
    }
}
