package ui.steps;

import ui.pages.LoginPage;
import ui.pages.UserDashboard;

public class UiStep {
    private LoginPage loginPage = new LoginPage();
    private UserDashboard dashboard = new UserDashboard();

    public void loginAsUser(String username, String password) {
        loginPage.open().login(username, password);
    }

    public void loginAsAdmin() {
        loginPage.open().login("admin", "admin");
    }

    public UserDashboard openDashboard() {
        return dashboard.open();
    }
}
