import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.JOptionPane;
/**
 * This method handles user verification
 * @author zonickba
 *
 */
public class ChessbaseUserService {
	
	private static final Random RANDOM = new SecureRandom();
	private static final Base64.Encoder enc = Base64.getEncoder();
	private static final Base64.Decoder dec = Base64.getDecoder();
	private ChessbaseConnectionService dbService = null;

	/**
	 * This method handles logging in. **CURRENTLY NON FUNCTIONAL**
	 * @param username
	 * @param password
	 * @return
	 */
	public boolean login(String username, String password) {
	
		Connection c = dbService.getConnection();
		PreparedStatement stmt;
		String query = "SELECT PasswordSalt, PasswordHash FROM [Users] WHERE Username = ?";
		
		try {
			stmt = c.prepareStatement(query);
			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();
			rs.next();
			System.out.println(username);
			byte[] salt = dec.decode(rs.getString("PasswordSalt"));
			String hash = hashPassword(salt, password);
			if(rs.getString("PasswordHash").equals(hash)) {
				return true;				
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JOptionPane.showMessageDialog(null, "Login failed");
		return false;
	}

	/**
	 * This method handles registering **CURRENTLY NON FUNCTIONAL**
	 * @param username
	 * @param password
	 * @return
	 */
	public boolean register(String username, String password) {
		byte[] salt = getNewSalt();
		String hash = hashPassword(salt, password);
		Connection c = dbService.getConnection();
		try {
			CallableStatement cs = c.prepareCall("{? = call Register(?, ?, ?)}");
			cs.setString(2, username);
			cs.setString(3, getStringFromBytes(salt));
			cs.setString(4, hash);
			cs.registerOutParameter(1, Types.INTEGER);
			cs.execute();
			int returnVal = cs.getInt(1);
			if (returnVal != 0) {
				JOptionPane.showMessageDialog(null, "Registration failed");
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public byte[] getNewSalt() {
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		return salt;
	}

	public String getStringFromBytes(byte[] data) {
		return enc.encodeToString(data);
	}

	public String hashPassword(byte[] salt, String password) {

		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
		SecretKeyFactory f;
		byte[] hash = null;
		try {
			f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			hash = f.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException e) {
			JOptionPane.showMessageDialog(null, "An error occurred during password hashing. See stack trace.");
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			JOptionPane.showMessageDialog(null, "An error occurred during password hashing. See stack trace.");
			e.printStackTrace();
		}
		return getStringFromBytes(hash);
	}
	
	
	
}
