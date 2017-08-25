package com.epam.auction.controller;

import com.epam.auction.command.AbstractCommand;
import com.epam.auction.receiver.RequestConstant;
import com.epam.auction.command.RequestContent;
import com.epam.auction.command.CommandFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "MainController", urlPatterns = {"/controller"})
public class MainController extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestContent requestContent = new RequestContent();

        requestContent.extractValues(request);

        CommandFactory commandFactory = new CommandFactory();
        AbstractCommand command = commandFactory.initCommand(requestContent);

        PageGuide pageGuide = command.execute(requestContent);

        requestContent.setSessionAttribute(RequestConstant.CURRENT_PAGE, pageGuide.getPageAddress());

        requestContent.insertAttributes(request);

        if (TransferMethod.FORWARD.equals(pageGuide.getTransferMethod())) {
            request.getRequestDispatcher(pageGuide.getPageAddress()).forward(request, response);
        } else {
            response.sendRedirect(pageGuide.getPageAddress());
        }
    }

}