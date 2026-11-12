/** Checked: the card-reader file is damaged. The ORIGINAL cause (e.g. NumberFormatException) is kept. */
public class CardDataException extends Exception {
    public CardDataException(String message, Throwable cause) { super(message, cause); }
}
