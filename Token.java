public class Token {
	public String tokenType;
	public String tokenValue;
	
	public Token(String tokenType, String tokenValue) {
		this.tokenType = tokenType;
		this.tokenValue = tokenValue;
	}
	
	public String toString() {
		return (tokenValue);
	}
}
