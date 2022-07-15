package it.atlantica.AlgoDemo.Model;

public class DataTrans {
	
	private String mnemonic;
	
	private String reciver;
	
	private int amount;
	
	private String note;

	public DataTrans(String mnemonic, String reciver, int amount, String note) {
		super();
		this.mnemonic = mnemonic;
		this.reciver = reciver;
		this.amount = amount;
		this.note = note;
	}

	public String getMnemonic() {
		return mnemonic;
	}

	public void setMnemonic(String mnemonic) {
		this.mnemonic = mnemonic;
	}

	public String getReciver() {
		return reciver;
	}

	public void setReciver(String reciver) {
		this.reciver = reciver;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	@Override
	public String toString() {
		return "DataTrans [mnemonic=" + mnemonic + ", reciver=" + reciver + ", amount=" + amount + ", note=" + note
				+ "]";
	}
	
	
	

}
