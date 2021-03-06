package com.vaguehope.dlnatoad.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

import com.vaguehope.dlnatoad.dlnaserver.ContentGroup;
import com.vaguehope.dlnatoad.dlnaserver.ContentNode;
import com.vaguehope.dlnatoad.dlnaserver.ContentTree;

public class IndexServlet extends HttpServlet {

	private static final long serialVersionUID = -8907271726001369264L;

	private final ContentTree contentTree;

	public IndexServlet (final ContentTree contentTree) {
		this.contentTree = contentTree;
	}

	@Override
	protected void doGet (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		if (path == null || path.length() < 1 || "/".equals(path)) {
			path = ContentGroup.ROOT.getId();
		}
		else if (path.startsWith("/")) {
			path = path.substring(1);
		}

		final ContentNode contentNode = this.contentTree.getNode(path);
		if (contentNode == null) {
			returnStatus(resp, HttpServletResponse.SC_NOT_FOUND, "Not found: " + path);
			return;
		}
		printDir(resp, contentNode);
	}

	private void printDir (final HttpServletResponse resp, final ContentNode contentNode) throws IOException {
		final List<Container> dirs = new ArrayList<Container>();
		final List<ContentNode> items = new ArrayList<ContentNode>();
		final Container dirNodeContainer = contentNode.getContainer();
		synchronized (dirNodeContainer) {
			for (final Container c : dirNodeContainer.getContainers()) {
				dirs.add(c);
			}
			for (final Item item : dirNodeContainer.getItems()) {
				items.add(this.contentTree.getNode(item.getId()));
			}
		}
		Collections.sort(items, NodeOrder.TITLE_OR_NAME);

		resp.setContentType("text/html; charset=utf-8");
		final PrintWriter w = resp.getWriter();

		w.print("<html><body><h3>");
		w.print(dirNodeContainer.getTitle());
		w.print(" (");
		w.print(dirs.size());
		w.println(" dirs, ");
		w.print(items.size());
		w.println(" items)</h3><ul>");

		for (final Container dir : dirs) {
			w.print("<li><a href=\"/index/");
			w.print(dir.getId());
			w.print("\">");
			w.print(dir.getTitle());
			w.println("</a></li>");
		}

		for (final ContentNode node : items) {
			w.print("<li><a href=\"/");
			w.print(node.getId());
			w.print("\" download=\"");
			w.print(node.getFile().getName());
			w.print("\">");
			w.print(node.getFile().getName());
			w.println("</a></li>");
		}

		w.println("</ul></html></body>");
	}

	private enum NodeOrder implements Comparator<ContentNode> {
		TITLE_OR_NAME {
			@Override
			public int compare (final ContentNode a, final ContentNode b) {
				return nameOf(a).compareToIgnoreCase(nameOf(b));
			}

			private String nameOf (final ContentNode n) {
				return n.getItem() != null ? n.getItem().getTitle() :
						n.getContainer() != null ? n.getContainer().getTitle() :
								n.getFile() != null ? n.getFile().getName() : "";
			}
		};

		@Override
		public abstract int compare (final ContentNode o1, final ContentNode o2);
	}

	private static void returnStatus (final HttpServletResponse resp, final int status, final String msg) throws IOException {
		resp.reset();
		resp.setContentType("text/plain");
		resp.setStatus(status);
		resp.getWriter().println(msg);
	}

}
