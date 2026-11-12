package campus.reg;

import campus.reg.dao.InMemoryRegistrationDao;
import campus.reg.dao.JdbcRegistrationDao;
import campus.reg.dao.RegistrationDao;
import campus.reg.service.RegistrationService;
import campus.reg.ui.MainFrame;
import javax.swing.SwingUtilities;

/**
 * Entry point.
 *   MySQL : java -cp "out;mysql-connector-j-8.4.0.jar" campus.reg.App -Ddb.url=... (see README)
 *   Demo  : java -Ddemo=true -cp out campus.reg.App    (no database, sample data in memory)
 */
public class App {
    public static void main(String[] args) {
        RegistrationDao dao;
        if (Boolean.getBoolean("demo")) {
            dao = InMemoryRegistrationDao.withDemoData();
        } else {
            String url = System.getProperty("db.url", "jdbc:mysql://localhost:3306/campus_events");
            dao = new JdbcRegistrationDao(url, System.getProperty("db.user", "root"), System.getProperty("db.pwd", ""));
        }
        RegistrationService service = new RegistrationService(dao);
        SwingUtilities.invokeLater(() -> new MainFrame(service, 10).setVisible(true));
    }
}
