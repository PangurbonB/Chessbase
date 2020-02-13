import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
/**
 * This method handles connection to the database, along with the GUI
 * @author zonickba
 *
 */
public class ChessbaseConnectionService {
	
	private final String connectionURL = "jdbc:sqlserver://${dbServer};databaseName=${dbName};user=${user};password={${pass}}";

	private Connection connection = null;
	private String serverName, databaseName;
	private JFrame loginFrame;
	private JFrame useFrame;
	private JTextField userBox;
	private JTextField passBox;
	private JTextField searchBox;
	private JTable table;
	private JComboBox selectionMenu;
	private JComboBox constraintMenu;
	private String[] tableNames = {"Judge", "Person", "Player", "Tournament", "MatchHost", "ChessMatch", "ChessMove", "CompetesIn"};
	private String[] searchOptions = {"None", "ELO Search", "Full Name Search", "Tournament Name Search"};
	private String[] searchTables = {"Player", "Person", "Tournament"};
	private String[] searchWheres = {"ELO", "FullName", "TournamentName"};
	private JButton searchButton;
	
	/**
	 * A basic constructor for our connection service.
	 * @param serverName the name of the server we are connecting to
	 * @param databaseName our database name
	 */
	public ChessbaseConnectionService(String serverName, String databaseName) {
		this.serverName = serverName;
		this.databaseName = databaseName;
		this.loginFrame = makeLoginDialog();
	}
	
	/**
	 * This is the method that connects to the database.
	 * @param user username
	 * @param pass password
	 * @return
	 */
	public boolean connect(String user, String pass) {
		
		String connectionString = connectionURL.replace("${dbServer}", serverName).replace("${dbName}", databaseName).replace("${user}", user).replace("{${pass}}", pass);
		System.out.println(connectionString);
		try {
			Connection c = DriverManager.getConnection(connectionString);
			this.connection = c;
			return true;
			
		}
		catch(SQLException e) {
			System.out.println("Invalid Login Credentials");
		}
		return false;	
	}
	
	/**
	 * @return a connection object (our connection)
	 */
	public Connection getConnection() {
		return this.connection;
	}
	
	/**
	 * closes the connection
	 */
	public void closeConnection() {
		try {
			this.connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}	
	
	/**
	 * Opens the 'use' frame, where we can select from and edit our data.
	 */
	public void openUseFrame() {
		JFrame frame = new JFrame("Chessbase Query Window");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		frame.add(panel, BorderLayout.CENTER);
		JPanel topPanel = new JPanel();
		panel.add(topPanel, BorderLayout.PAGE_START);
		this.loginFrame.dispose();
		
		try {
			String query = "SELECT Username, Pswd, FullName, JoinDate FROM Person"; //The starting info, in there by default.
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			
			//System.out.println("Made Query");
			
			JTable table = new JTable(new DefaultTableModel());
			
			int i = 0;
			DefaultTableModel model = (DefaultTableModel) table.getModel();
			
			model.addColumn("Username"); 
			model.addColumn("Password");
			model.addColumn("Full Name");
			model.addColumn("Join Date");
			while(rs.next()) {
				i++;
				
				model.addRow(new Object[]{rs.getString("Username"), rs.getString("Pswd"), rs.getString("FullName"), rs.getString("JoinDate")});
				
				System.out.println(rs.getString("Username") + " : " + rs.getString("Pswd") + " : " +rs.getString("FullName") + " : " +rs.getString("JoinDate"));
			}
			JScrollPane scrollPane = new JScrollPane(table);
			
			this.table = table;
			table.setDefaultEditor(Object.class, null);
			table.getTableHeader().setReorderingAllowed(false); 
			
			JComboBox tableList = new JComboBox(tableNames);
			tableList.setSelectedIndex(1);
			tableList.addActionListener(new TableSwitchListener());
			this.selectionMenu = tableList;
			
			JComboBox constraintList = new JComboBox(searchOptions);
			constraintList.setSelectedIndex(0);
			this.constraintMenu = constraintList;
			
			JTextField entryBox = new JTextField(50);
			this.searchBox = entryBox;
			
			JButton searchButton = new JButton("Search");
			searchButton.addActionListener(new SearchButtonListener());
			this.searchButton = searchButton;
			
			topPanel.add(tableList, BorderLayout.WEST);
			topPanel.add(constraintList,BorderLayout.CENTER);
			topPanel.add(entryBox, BorderLayout.EAST);
			topPanel.add(searchButton, BorderLayout.EAST);
			panel.add(scrollPane, BorderLayout.PAGE_END);
			frame.pack();
			frame.setVisible(true);
			//System.out.println("Returned");
			return;
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		this.useFrame = frame;
		closeConnection();
	}
	
	/**
	 * This method makes the login dialog, where the user enters login credentials to acces our database.
	 * @return
	 */
	public JFrame makeLoginDialog() {
		JFrame frame = new JFrame("Chessbase Connection Window");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		frame.add(panel, BorderLayout.CENTER);
		
		JTextField usernameEntry = new JTextField(50);
		usernameEntry.setText("SodaBaseUserzonickba20"); //default credentials
		JTextField passwordEntry = new JTextField(50);
		passwordEntry.setText("Password123");
		JButton connectButton = new JButton("Connect");
		connectButton.addActionListener(new ConnectActionListener());
		
		panel.add(usernameEntry);
		panel.add(passwordEntry);
		panel.add(connectButton);
		
		this.userBox = usernameEntry;
		this.passBox = passwordEntry;
		
		frame.pack();
		frame.setVisible(true);
		return frame;
	}
	
	/**
	 * This action listener is associated with the connection button, and calls the 
	 * relevant method when the button is hit
	 * @author zonickba
	 *
	 */
	private class ConnectActionListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean connectionSuccessful = connectFromFrame();
			if(!connectionSuccessful) {
				System.out.println("Failed Connection");
				return;
			}
			openUseFrame();
		}

		private void openUseFrame() {
			ChessbaseConnectionService.this.openUseFrame();
		}

	}
	
	/**
	 * This method gets called when the 'connect' button gets hit. Takes info from the user and password
	 * boxes and connects with it.
	 * @return
	 */
	private boolean connectFromFrame() {
		
		String user = this.userBox.getText();
		String pass = this.passBox.getText();
		
		return connect(user, pass);
	}
	
	/**
	 * This button is attached to the 'Search' button, and executes the search when the button is hit.
	 * @author zonickba
	 *
	 */
	private class SearchButtonListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			String query = "SELECT * FROM ";
			PreparedStatement ps;
			if(constraintMenu.getSelectedIndex() == 0) {
				System.out.println("Bad Selection!");
				return;
			}
			try { //This part goes and executes the query we want executed.
				query = query + searchTables[constraintMenu.getSelectedIndex() -1] + " WHERE " + searchWheres[constraintMenu.getSelectedIndex() - 1] + " = '" +searchBox.getText()+"'";
				System.out.println(query);
				ps = connection.prepareStatement(query);
				ResultSet rs = ps.executeQuery();

				DefaultTableModel model = (DefaultTableModel) table.getModel();
				model.setColumnCount(0);
				model.setRowCount(0);
				

				ResultSetMetaData rsmd = rs.getMetaData();
				ArrayList<String> names = new ArrayList<String>();
				int columnCount = rsmd.getColumnCount();

				
				for(int i = 0; i < columnCount; i++) {
					names.add(rsmd.getColumnName(i+1));
					model.addColumn(rsmd.getColumnName(i+1));
				}
				
				while(rs.next()) {
					Object[] data = new Object[names.size()];
					for(int j = 0; j < names.size(); j++) {
						data[j] = rs.getString(j+1);
					}
					model.addRow(data);
				}
				
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			
		}
		
	}
	
	/**
	 * When someone switches what table they are viewing, this listener activates, and updates the table
	 * displayed.
	 * @author zonickba
	 *
	 */
	private class TableSwitchListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			Object selected = selectionMenu.getSelectedItem();
			
			String query = "SELECT * FROM "+ selected.toString();
			
			Statement s;
			
			try {
				s = connection.createStatement();
				ResultSet rs = s.executeQuery(query);
				ResultSetMetaData rsmd = rs.getMetaData();
				ArrayList<String> names = new ArrayList<String>();
				int columnCount = rsmd.getColumnCount();
			
				DefaultTableModel model = (DefaultTableModel) table.getModel();
				model.setColumnCount(0);
				model.setRowCount(0);
				for(int i = 0; i < columnCount; i++) {
					names.add(rsmd.getColumnName(i+1));
					model.addColumn(rsmd.getColumnName(i+1));
				}
				
				int i = 0;
				while(rs.next()) {
					i++;
					Object[] data = new Object[names.size()];
					for(int j = 0; j < names.size(); j++) {
						data[j] = rs.getString(j+1);
					}
					model.addRow(data);
				}
				
				
				System.out.println(names);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			System.out.println(query);
		}
		
	}

}
