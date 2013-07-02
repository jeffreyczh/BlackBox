package redirector;

/**
 *
 * @author
 */
public class RedirectorDriver {
    public static void main(String[] args) {
            try {
                    new Redirector(args[0]);
            } catch (Exception e) {
                    System.out.println("[Error] Fail to start the server");
                    System.out.println(e.getMessage());
            }
    }
}
