import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class ChessbaseConnectionService {
	
	private final String connectionURL = "jdbc:sqlserver://${dbServer};databaseName=${dbName};user=${user};password={${pass}}";

	private Connection connection = null;
	private String serverName, databaseName;
	private JFrame loginFrame;
	private JFrame useFrame;
	private JTextField userBox;
	private JTextField passBox;
	
	public ChessbaseConnectionService(String serverName, String databaseName) {
		this.serverName = serverName;
		this.databaseName = databaseName;
		this.loginFrame = makeLoginDialog();
	}
	
	private boolean connectFromFrame() {
		
		String user = this.userBox.getText();
		String pass = this.passBox.getText();
		
		return connect(user, pass);
	}
	
	
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
	
	public Connection getConnection() {
		return this.connection;
	}
	
	public void closeConnection() {
		try {
			this.connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void openUseFrame() {
		JFrame frame = new JFrame("Chessbase Query Window");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		frame.add(panel, BorderLayout.CENTER);
		this.loginFrame.dispose();
		
		
		try {
			String query = "SELECT Username, Pswd, FullName, JoinDate FROM Person";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			
			System.out.println("Made Query");
			
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
			table.setFillsViewportHeight(true);
			frame.add(scrollPane);
			frame.pack();
			frame.setVisible(true);
			System.out.println("Returned");
			return;
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		this.useFrame = frame;
		closeConnection();
	}
	
	public JFrame makeLoginDialog() {
		JFrame frame = new JFrame("Chessbase Connection Window");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		frame.add(panel, BorderLayout.CENTER);
		
		JTextField usernameEntry = new JTextField(50);
		usernameEntry.setText("SodaBaseUserzonickba20");
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
	
}
