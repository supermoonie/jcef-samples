package com.github.supermoonie.cef;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemInfo;
import com.github.supermoonie.cef.handler.FileHandler;
import com.github.supermoonie.cef.handler.SvgConvertHandler;
import com.github.supermoonie.cef.ui.MenuBar;
import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.JCefLoader;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefAuthCallback;
import org.cef.callback.CefRequestCallback;
import org.cef.handler.*;
import org.cef.misc.BoolRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.cef.network.CefURLRequest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * This is a simple example application using JCEF.
 * It displays a JFrame with a JTextField at its top and a CefBrowser in its
 * center. The JTextField is used to enter and assign an URL to the browser UI.
 * No additional handlers or callbacks are used in this example.
 * <p>
 * The number of used JCEF classes is reduced (nearly) to its minimum and should
 * assist you to get familiar with JCEF.
 * <p>
 * For a more feature complete example have also a look onto the example code
 * within the package "tests.detailed".
 */
public class App extends JFrame {
    private static final long serialVersionUID = -5570653778104813836L;
    private final JTextField address_;
    private final CefApp cefApp_;
    private final CefClient client_;
    private final CefBrowser browser_;
    private final Component browerUI_;
    private boolean browserFocus_ = true;

    /**
     * To display a simple browser window, it suffices completely to create an
     * instance of the class CefBrowser and to assign its UI component to your
     * application (e.g. to your content pane).
     * But to be more verbose, this CTOR keeps an instance of each object on the
     * way to the browser UI.
     */
    private App(String startURL, boolean useOSR, boolean isTransparent) throws IOException {
        // (1) The entry point to JCEF is always the class CefApp. There is only one
        //     instance per application and therefore you have to call the method
        //     "getInstance()" instead of a CTOR.
        //
        //     CefApp is responsible for the global CEF context. It loads all
        //     required native libraries, initializes CEF accordingly, starts a
        //     background task to handle CEF's message loop and takes care of
        //     shutting down CEF after disposing it.
        CefApp.addAppHandler(new CefAppHandlerAdapter(null) {
            @Override
            public void stateHasChanged(CefAppState state) {
                // Shutdown the app if the native CEF part is terminated
                if (state == CefAppState.TERMINATED) {
                    System.exit(0);
                }
            }
        });
        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = useOSR;
        cefApp_ = JCefLoader.installAndLoadCef(settings);

        // (2) JCEF can handle one to many browser instances simultaneous. These
        //     browser instances are logically grouped together by an instance of
        //     the class CefClient. In your application you can create one to many
        //     instances of CefClient with one to many CefBrowser instances per
        //     client. To get an instance of CefClient you have to use the method
        //     "createClient()" of your CefApp instance. Calling an CTOR of
        //     CefClient is not supported.
        //
        //     CefClient is a connector to all possible events which come from the
        //     CefBrowser instances. Those events could be simple things like the
        //     change of the browser title or more complex ones like context menu
        //     events. By assigning handlers to CefClient you can control the
        //     behavior of the browser. See tests.detailed.MainFrame for an example
        //     of how to use these handlers.
        client_ = cefApp_.createClient();

        // (3) One CefBrowser instance is responsible to control what you'll see on
        //     the UI component of the instance. It can be displayed off-screen
        //     rendered or windowed rendered. To get an instance of CefBrowser you
        //     have to call the method "createBrowser()" of your CefClient
        //     instances.
        //
        //     CefBrowser has methods like "goBack()", "goForward()", "loadURL()",
        //     and many more which are used to control the behavior of the displayed
        //     content. The UI is held within a UI-Compontent which can be accessed
        //     by calling the method "getUIComponent()" on the instance of CefBrowser.
        //     The UI component is inherited from a java.awt.Component and therefore
        //     it can be embedded into any AWT UI.
        browser_ = client_.createBrowser(startURL, useOSR, isTransparent);
        browerUI_ = browser_.getUIComponent();

        // (4) For this minimal browser, we need only a text field to enter an URL
        //     we want to navigate to and a CefBrowser window to display the content
        //     of the URL. To respond to the input of the user, we're registering an
        //     anonymous ActionListener. This listener is performed each time the
        //     user presses the "ENTER" key within the address field.
        //     If this happens, the entered value is passed to the CefBrowser
        //     instance to be loaded as URL.
        address_ = new JTextField(startURL, 100);
        address_.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browser_.loadURL(address_.getText());
            }
        });

        // Update the address field when the browser URL changes.
        client_.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                App.this.setTitle(title);
            }

            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                address_.setText(url);
            }
        });

        // Clear focus from the browser when the address field gains focus.
        address_.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!browserFocus_) {
                    return;
                }
                browserFocus_ = false;
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                address_.requestFocus();
            }
        });

        // Clear focus from the address field when the browser gains focus.
        client_.addFocusHandler(new CefFocusHandlerAdapter() {
            @Override
            public void onGotFocus(CefBrowser browser) {
                if (browserFocus_) {
                    return;
                }
                browserFocus_ = true;
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                browser.setFocus(true);
            }

            @Override
            public void onTakeFocus(CefBrowser browser, boolean next) {
                browserFocus_ = false;
            }
        });

        client_.addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
                return super.onBeforeBrowse(browser, frame, request, user_gesture, is_redirect);
            }

            @Override
            public boolean onOpenURLFromTab(CefBrowser browser, CefFrame frame, String target_url, boolean user_gesture) {
                return super.onOpenURLFromTab(browser, frame, target_url, user_gesture);
            }

            @Override
            public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame, CefRequest request, boolean isNavigation, boolean isDownload, String requestInitiator, BoolRef disableDefaultHandling) {
                return new CefResourceRequestHandlerAdapter() {
                    @Override
                    public CefCookieAccessFilter getCookieAccessFilter(CefBrowser browser, CefFrame frame, CefRequest request) {
                        return super.getCookieAccessFilter(browser, frame, request);
                    }

                    @Override
                    public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
                        return super.onBeforeResourceLoad(browser, frame, request);
                    }

                    @Override
                    public CefResourceHandler getResourceHandler(CefBrowser browser, CefFrame frame, CefRequest request) {
                        return super.getResourceHandler(browser, frame, request);
                    }

                    @Override
                    public void onResourceRedirect(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response, StringRef new_url) {
                        super.onResourceRedirect(browser, frame, request, response, new_url);
                    }

                    @Override
                    public boolean onResourceResponse(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response) {
                        return super.onResourceResponse(browser, frame, request, response);
                    }

                    @Override
                    public void onResourceLoadComplete(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response, CefURLRequest.Status status, long receivedContentLength) {
                        super.onResourceLoadComplete(browser, frame, request, response, status, receivedContentLength);
                    }

                    @Override
                    public void onProtocolExecution(CefBrowser browser, CefFrame frame, CefRequest request, BoolRef allowOsExecution) {
                        super.onProtocolExecution(browser, frame, request, allowOsExecution);
                    }
                };
            }

            @Override
            public boolean getAuthCredentials(CefBrowser browser, String origin_url, boolean isProxy, String host, int port, String realm, String scheme, CefAuthCallback callback) {
                return super.getAuthCredentials(browser, origin_url, isProxy, host, port, realm, scheme, callback);
            }

            @Override
            public boolean onQuotaRequest(CefBrowser browser, String origin_url, long new_size, CefRequestCallback callback) {
                return super.onQuotaRequest(browser, origin_url, new_size, callback);
            }

            @Override
            public boolean onCertificateError(CefBrowser browser, CefLoadHandler.ErrorCode cert_error, String request_url, CefRequestCallback callback) {
                return super.onCertificateError(browser, cert_error, request_url, callback);
            }

            @Override
            public void onPluginCrashed(CefBrowser browser, String pluginPath) {
                super.onPluginCrashed(browser, pluginPath);
            }

            @Override
            public void onRenderProcessTerminated(CefBrowser browser, TerminationStatus status) {
                super.onRenderProcessTerminated(browser, status);
            }
        });

        CefMessageRouter fileRouter = CefMessageRouter.create(new CefMessageRouter.CefMessageRouterConfig("fileQuery", "cancelFilerQuery"));
        fileRouter.addHandler(new FileHandler(this), false);
        CefMessageRouter svgRouter = CefMessageRouter.create(new CefMessageRouter.CefMessageRouterConfig("svgQuery", "cancelSvgQuery"));
        svgRouter.addHandler(new SvgConvertHandler(), false);
        client_.addMessageRouter(fileRouter);
        client_.addMessageRouter(svgRouter);

        MenuBar menuBar = new MenuBar(this, browser_);
        setJMenuBar(menuBar);

        // (5) All UI components are assigned to the default content pane of this
        //     JFrame and afterwards the frame is made visible to the user.
        getContentPane().add(address_, BorderLayout.NORTH);
        getContentPane().add(browerUI_, BorderLayout.CENTER);
        pack();
        setSize(1200, 800);
        setResizable(true);
        setVisible(true);
        setLocationRelativeTo(null);

        // (6) To take care of shutting down CEF accordingly, it's important to call
        //     the method "dispose()" of the CefApp instance if the Java
        //     application will be closed. Otherwise you'll get asserts from CEF.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                CefApp.getInstance().dispose();
                dispose();
            }
        });
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
//        // Perform startup initialization on platforms that require it.
//        if (!CefApp.startup(args)) {
//            System.out.println("Startup initialization failed!");
//            return;
//        }
        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
        // enable window decorations
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        FlatLightLaf.install();
        UIManager.setLookAndFeel(FlatLightLaf.class.getName());

        // The simple example application is created as anonymous class and points
        // to Google as the very first loaded page. Windowed rendering mode is used by
        // default. If you want to test OSR mode set |useOsr| to true and recompile.
        boolean useOsr = false;
        new App("file:///Users/supermoonie/IdeaProjects/jcef-macos/src/main/resources/Main.html", useOsr, false);
    }
}