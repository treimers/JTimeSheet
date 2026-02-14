package treimers.net.jtimesheet.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;

import javafx.application.HostServices;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.web.WebView;
import treimers.net.jtimesheet.model.Language;

/**
 * Controller for the JTimeSheet help / user manual dialog.
 * Displays markdown-generated HTML in a WebView with a ListView for navigation.
 * Uses ListView instead of TreeView to avoid JavaFX TreeView IndexOutOfBoundsException on macOS.
 * Supports English and German via separate content directories.
 */
public class HelpController implements Initializable {

    private static final String HELP_BASE = "/treimers/net/jtimesheet/help/";

    @FXML
    private ListView<String> listView;
    @FXML
    private SplitPane splitPane;
    @FXML
    private WebView webView;

    private final List<String> linkIds = new ArrayList<>();
    private final Map<String, Integer> urlToIndex = new HashMap<>();
    private boolean isAdjusting;
    private HostServices hostServices;
    private ResourceBundle messages;
    private String languageTag = Language.ENGLISH.getCode();

    /**
     * Sets the host services used to open external URLs in the system browser.
     *
     * @param hostServices the host services
     */
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    /**
     * Sets the resource bundle for translating list item labels.
     *
     * @param messages the messages bundle
     */
    public void setMessages(ResourceBundle messages) {
        this.messages = messages;
    }

    /**
     * Sets the language for help content (en or de).
     * Must be called before or during initialize.
     *
     * @param language the language
     */
    public void setLanguage(Language language) {
        this.languageTag = language != null ? language.getCode() : Language.ENGLISH.getCode();
    }

    private String i18n(String key) {
        return messages != null && messages.containsKey(key) ? messages.getString(key) : key;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        splitPane.setDividerPositions(0.25);
        SplitPane.setResizableWithParent(listView, false);
        listView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            if (isAdjusting || newIdx == null || newIdx.intValue() < 0) {
                return;
            }
            handleListSelection(newIdx.intValue());
        });
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                injectLinkInterceptor();
            }
        });
        webView.getEngine().locationProperty().addListener(this::handleLinkEvent);
    }

    /**
     * Builds the help list and loads the first page. Call after setLanguage and setMessages.
     */
    public void initContent() {
        linkIds.clear();
        urlToIndex.clear();
        String[] labels = {
            i18n("help.toc.welcome"),
            i18n("help.toc.intro"),
            i18n("help.toc.gettingstarted"),
            i18n("help.toc.activities"),
            i18n("help.toc.manage"),
            i18n("help.toc.file")
        };
        String[] links = {
            "1__welcome",
            "1_1_intro",
            "1_2_gettingstarted",
            "1_3_activities",
            "1_4_manage",
            "1_5_file"
        };
        for (int i = 0; i < links.length; i++) {
            linkIds.add(links[i]);
            URL resource = getClass().getResource(HELP_BASE + languageTag + "/" + links[i] + ".html");
            if (resource != null) {
                urlToIndex.put(resource.toExternalForm(), i);
            }
        }
        ObservableList<String> items = FXCollections.observableArrayList(labels);
        listView.setItems(items);
        listView.getSelectionModel().select(0);
        loadPage(0);
    }

    private void handleListSelection(int index) {
        if (index >= 0 && index < linkIds.size()) {
            try {
                isAdjusting = true;
                loadPage(index);
            } finally {
                isAdjusting = false;
            }
        }
    }

    private void loadPage(int index) {
        if (index < 0 || index >= linkIds.size()) {
            return;
        }
        String linkId = linkIds.get(index);
        URL resource = getClass().getResource(HELP_BASE + languageTag + "/" + linkId + ".html");
        if (resource != null) {
            webView.getEngine().load(resource.toExternalForm());
        }
    }

    private void injectLinkInterceptor() {
        Document doc = webView.getEngine().getDocument();
        if (doc == null) {
            return;
        }
        NodeList nodeList = doc.getElementsByTagName("a");
        for (int i = 0; i < nodeList.getLength(); i++) {
            EventTarget link = (EventTarget) nodeList.item(i);
            link.addEventListener("click", evt -> {
                EventTarget target = evt.getCurrentTarget();
                Element anchor = (Element) target;
                String href = anchor.getAttribute("href");
                if (href != null && (href.startsWith("http://") || href.startsWith("https://"))) {
                    javafx.application.Platform.runLater(() -> {
                        if (hostServices != null) {
                            hostServices.showDocument(href);
                        }
                    });
                    evt.preventDefault();
                }
            }, false);
        }
    }

    private void handleLinkEvent(Observable o) {
        isAdjusting = true;
        try {
            if (!(o instanceof ReadOnlyStringProperty)) {
                return;
            }
            ReadOnlyStringProperty property = (ReadOnlyStringProperty) o;
            String value = property.getValue();
            if (value == null) {
                return;
            }
            Integer index = urlToIndex.get(value);
            if (index != null && index >= 0 && index < listView.getItems().size()) {
                listView.getSelectionModel().clearAndSelect(index);
            }
        } finally {
            isAdjusting = false;
        }
    }
}
