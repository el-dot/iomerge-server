package pl.kbieron.iomerge.server.ui.movementReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.kbieron.iomerge.server.deviceAbstraction.VirtualScreen;
import pl.kbieron.iomerge.server.gesture.GestureRecorder;
import pl.kbieron.iomerge.server.ui.InvisibleJFrame;

import javax.annotation.PostConstruct;
import javax.swing.Timer;
import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import static java.awt.event.MouseEvent.BUTTON3;


@Component
public class MouseTrapReader extends InvisibleJFrame implements MovementReader, MouseListener, MouseMotionListener {

	private final Log log = LogFactory.getLog(MouseTrapReader.class);

	private Point center;

	private Point oldMouseLocation;

	private boolean reading;

	private Timer timer;

	private MovementListener movementListener;

	@Autowired
	private VirtualScreen virtualScreen;

	@Autowired
	private GestureRecorder gestureRecorder;

	public MouseTrapReader() {
		super("MouseTrapReader");
	}

	@PostConstruct
	public void init() {
		movementListener = virtualScreen;
		Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration().getBounds();
		setLocation(bounds.x, bounds.y);
		setSize(bounds.height, bounds.width);

		timer = new Timer(20, a -> readMove());

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent keyEvent) {
				switch (keyEvent.getKeyCode()) {
					case KeyEvent.VK_ESCAPE:
						virtualScreen.exit();
				}
			}
		});
		addMouseListener(this);
	}

	private void readMove() {
		if ( !reading ) return;
		Point move = MouseInfo.getPointerInfo().getLocation();
		move.translate(-center.x, -center.y);

		if ( move.x != 0 || move.y != 0 ) {
			movementListener.moveMouse(move.x, move.y);
			log.info(move);
		}
		centerPointer();
	}

	private void centerPointer() {
		try {
			new Robot().mouseMove(center.x, center.y);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startReading() {
		if ( reading ) return;
		reading = true;

		oldMouseLocation = MouseInfo.getPointerInfo().getLocation();

		setVisible(true);

		center = getLocation();
		center.translate(getHeight() / 2, getWidth() / 2);
		centerPointer();

		timer.start();
	}

	@Override
	public void stopReading() {
		timer.stop();
		restoreMouseLocation();
		setVisible(false);
		reading = false;
	}

	private void restoreMouseLocation() {
		try {
			new Robot().mouseMove(oldMouseLocation.x, oldMouseLocation.y);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void mouseClicked(MouseEvent mouseEvent) {// TODO check
	}

	@Override
	public void mousePressed(MouseEvent mouseEvent) {
		if ( mouseEvent.getButton() == BUTTON3 ) {
			gestureRecorder.mousePressed();
			movementListener = gestureRecorder;
		}
	}

	@Override
	public void mouseReleased(MouseEvent mouseEvent) {
		movementListener.mouseReleased();
		if ( mouseEvent.getButton() == BUTTON3 ) {
			movementListener = virtualScreen;
		}
	}

	@Override
	public void mouseEntered(MouseEvent mouseEvent) {}

	@Override
	public void mouseExited(MouseEvent mouseEvent) {}

	@Override
	public void mouseDragged(MouseEvent mouseEvent) {}

	@Override
	public void mouseMoved(MouseEvent mouseEvent) {
	}
}
