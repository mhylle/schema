package com.scandihealth.olympicsc.event.controller;

import com.scandihealth.olympicsc.activities.model.Activity;
import com.scandihealth.olympicsc.activities.model.UserForActivityPaintBean;
import com.scandihealth.olympicsc.commandsystem.CommandController;
import com.scandihealth.olympicsc.commandsystem.event.*;
import com.scandihealth.olympicsc.commandsystem.user.SaveUserCommand;
import com.scandihealth.olympicsc.data.DataManager;
import com.scandihealth.olympicsc.event.model.*;
import com.scandihealth.olympicsc.security.Authenticator;
import com.scandihealth.olympicsc.user.model.User;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.faces.Renderer;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.*;

@Name("eventController")
@Scope(ScopeType.SESSION)
@Restrict("#{identity.loggedIn}")
public class EventController implements Serializable {

    @In(create = true)
    private CommandController commandController;

    private EventRepository eventRepository;

    @DataModel
    private List<Event> eventList;

    @In(value = "event", create = true)
    @Out(value = "selectedEvent")
    private Event event;

    @DataModelSelection
//    @In(value="selectedEvent", required = false)
    @Out(required = false)
    private Event selectedEvent;

    @In
    Authenticator authenticator;
    private Boolean hasActivities;

    @In(create = true)
    private Renderer renderer;
    private UserForActivityPaintData usersForActivityPaintData;

    UserForActivityPaintBean userForActivityPaintBean;
    private List<User> userForActivity;

    @Create
    public void init() {
        eventRepository = new EventRepository();
    }

    @Factory("eventList")
    public void findEvents() {
        if (eventRepository == null) {
            eventRepository = new EventRepository();
        }
        eventList = eventRepository.getEvents();
        Collections.sort(eventList, new Comparator<Event>() {
            public int compare(Event o1, Event o2) {
                if (o1.getStart() != null && o2.getStart() != null) {
                    return o1.getStart().compareTo(o2.getStart());
                }

                return 0;
            }
        });
        if (eventList != null) {
            for (Event event1 : eventList) {
                addPartnerRequestToEvent(event1);
                addVegetarianRequestToEvent(event1);
            }
        }
    }

    private void addPartnerRequestToEvent(Event event) {
        User user = authenticator.getUser();
        DataManager dataManager = new DataManager();
        EventPartnerRequest eventPartnerRequest = dataManager.getEventPartnerRequest(user, event);
        if (eventPartnerRequest != null) {
            event.setPartnerRequest(eventPartnerRequest.isPartnerRequest());
        }
    }

    private void addVegetarianRequestToEvent(Event event) {
        User user = authenticator.getUser();
        DataManager dataManager = new DataManager();
        EventVegetarianRequest eventVegetarianRequest = dataManager.getEventVegetarianRequest(user, event);
        if (eventVegetarianRequest != null) {
            event.setVegetarianRequest(eventVegetarianRequest.getVegetarian());
        }
    }

    public String join() {
        User user = authenticator.getUser();
        System.out.println("User: " + user.getUserName() + "(" + user.getIduser() + ") tried joining " + selectedEvent.getName() + "(" + selectedEvent.getIdevent() + ")");
        String result = "";
        JoinEventCommand joinEventCommand = new JoinEventCommand(user, selectedEvent);
        commandController.executeCommand(joinEventCommand);
        if (selectedEvent.isCanRequestPartner() || selectedEvent.isCanRequestVegetarian() || selectedEvent.getActivities().size() > 0) {
            result = "showEventInfo";
        }

        SaveUserCommand saveUserCommand = new SaveUserCommand(user);
        commandController.executeCommand(saveUserCommand);
        System.out.println("join successfull " + result);
        return result;
    }

    public String cancel() {
        User user = authenticator.getUser();
        System.out.println("User: " + user.getUserName() + "(" + user.getIduser() + ") tried cancelling " + selectedEvent.getName() + "(" + selectedEvent.getIdevent() + ")");
        UnJoinEventCommand unJoinEventCommand = new UnJoinEventCommand(user, selectedEvent);
        commandController.executeCommand(unJoinEventCommand);
        SaveUserCommand saveUserCommand = new SaveUserCommand(user);
        commandController.executeCommand(saveUserCommand);
        System.out.println("cancel successful");
        return "";

    }


    public boolean isJoinedEvent() {
        return authenticator.getUser().hasJoinedEvent(selectedEvent);
    }


    public String createEvent() {
        CreateEventCommand createEventCommand = new CreateEventCommand(eventRepository, event);
        commandController.executeCommand(createEventCommand);
        if (hasActivities) {
            selectedEvent = event;
            return "createActivities";
        } else {
            return "eventCreated";
        }
    }


    public void deleteEvent() {
        DeleteEventCommand deleteEventCommand = new DeleteEventCommand(eventRepository, selectedEvent);
        commandController.executeCommand(deleteEventCommand);
    }

    public String doUpdateEvent() {
        if (selectedEvent.isCanRequestVegetarian()) {
            DataManager dataManager = new DataManager();
            EventVegetarianRequest eventVegetarianRequest = new EventVegetarianRequest();
            eventVegetarianRequest.setIduser(authenticator.getUser().getIduser());
            eventVegetarianRequest.setIdevent(selectedEvent.getIdevent());
            eventVegetarianRequest.setVegetarian(selectedEvent.getVegetarianRequest());
            dataManager.saveEventVegetarianRequest(eventVegetarianRequest);
        }

        if (selectedEvent.isCanRequestPartner()) {
            DataManager dataManager = new DataManager();
            EventPartnerRequest eventPartnerRequest = new EventPartnerRequest();
            eventPartnerRequest.setIduser(authenticator.getUser().getIduser());
            eventPartnerRequest.setIdevent(selectedEvent.getIdevent());
            eventPartnerRequest.setPartnerRequest(selectedEvent.isPartnerRequest());
            dataManager.saveEventPartnerRequest(eventPartnerRequest);
        }
        UpdateEventCommand updateEventCommand = new UpdateEventCommand(eventRepository, selectedEvent);
        commandController.executeCommand(updateEventCommand);
        FacesContext.getCurrentInstance().addMessage("eventList", new FacesMessage("Informationen er gemt."));
        return "";
    }

    public Boolean getHasActivities() {
        return hasActivities;
    }

    public void setHasActivities(Boolean hasActivities) {
        this.hasActivities = hasActivities;
    }


    public List<User> getUsers() {
        DataManager dataManager = new DataManager();
        return dataManager.getUserForEvent(selectedEvent);
    }

    public Event getSelectedEvent() {
        return selectedEvent;
    }

    public String addActivityToEvent() {
        return "createActivities";
    }

    public String updateEvent() {
        return "updateEvent";
    }

    public String showOlympicscExcel() {
        return "olympicscDataToExcell";
    }

    public String showExcelForAccounting() {
        return "excelForAccounting";
    }


    public String showDetails() {
        return "showDetails";
    }

    public Object showEventInfo() {
        return "showEventInfo";
    }

    public boolean isCanSign() {
        boolean result = true;
        Date date = new Date();
        Date signStart = selectedEvent.getSignstart();
        Date signEnd = selectedEvent.getSignend();

        if (signStart != null) {
            result = date.after(signStart);
        }
        if (signEnd != null) {
            result = result && date.before(signEnd);
        }

        return result;
    }

    public boolean isCanCancel() {
        Date unsignEnd = selectedEvent.getUnsignEnd();
        return unsignEnd == null || new Date().before(unsignEnd);
    }

    public boolean isHasJoined() {
        User user = authenticator.getUser();
        return user.hasJoinedEvent(selectedEvent);
    }

    public List<User> usersForActivity(Activity activity) {
        calculateUsersForActivity(activity);
        return userForActivity;
    }

    public void calculateUsersForActivity(Activity activity) {
        DataManager dataManager = new DataManager();
        userForActivity = dataManager.getUserForActivity(activity);
        List<User> tmpList = new ArrayList<User>();
        for (User user : userForActivity) {
            if (user.getEvents().contains(selectedEvent)) {
                tmpList.add(user);
            }
        }
        userForActivity.clear();
        for (User user : tmpList) {
            userForActivity.add(user);
        }
        usersForActivityPaintData = new UserForActivityPaintData();
        usersForActivityPaintData.setNumberOfUsers(userForActivity.size());
        usersForActivityPaintData.setActivity(activity);
    }

    public UserForActivityPaintData usersForActivityPaint(Activity activity) {
        calculateUsersForActivity(activity);
        return usersForActivityPaintData;
    }


    public List<String> getExcelColumnHeadersForAccounting() {
        List<String> result = new ArrayList<String>();
        result.add("Navn");
        result.add("Personalenr");
        result.add("Pris");
        return result;
    }

    public List<List<String>> getColumnDataForAccounting() {
        List<List<String>> result = new ArrayList<List<String>>();

        List<User> userList = getUsers();
        Collections.sort(userList, new Comparator<User>() {
            public int compare(User o1, User o2) {
                return o1.getFirstname().compareTo(o2.getFirstname());
            }
        });
        for (User user : userList) {
            List<String> column = new ArrayList<String>();
            column.add(user.getFirstname() + " " + user.getLastname());
            column.add(user.getEmployeeId());
            if (user.isPersonaleForening()) {
                column.add("" + selectedEvent.getMemberPrice());
            } else {
                column.add("" + selectedEvent.getNotMemberPrice());
            }

            // todo noshowprice
            result.add(column);
        }


        return result;
    }

    public List<String> getExcelColumnHeadersForOlympicsc() {
        List<String> result = new ArrayList<String>();
        result.add("Navn");
        List<Activity> activities = selectedEvent.getActivityList();
        Collections.sort(activities, new Comparator<Activity>() {
            public int compare(Activity o1, Activity o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (Activity activity : activities) {
            result.add(activity.getName());
        }
        return result;
    }

    public List<List<String>> getColumnDataForOlympicsc() {
        List<List<String>> result = new ArrayList<List<String>>();

        List<User> userList = getUsers();
        Collections.sort(userList, new Comparator<User>() {
            public int compare(User o1, User o2) {
                return o1.getFirstname().compareTo(o2.getFirstname());
            }
        });
        List<Activity> activities = selectedEvent.getActivityList();
        Collections.sort(activities, new Comparator<Activity>() {
            public int compare(Activity o1, Activity o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (User user : userList) {
            List<String> column = new ArrayList<String>();
            column.add(user.getFirstname() + " " + user.getLastname());
            for (Activity activity : activities) {
                if (user.getActivities().contains(activity)) {
                    column.add("1");
                } else {
                    column.add("0");
                }
            }
            result.add(column);
        }


        return result;
    }

    public void updateExtraInformation() {
        User user = authenticator.getUser();
        System.out.println("User: " + user.getUserName() + "(" + user.getIduser() + ") tried updating information.");
        doUpdateEvent();
    }

    public String unsign(User user) {
        System.out.println("Admin(" + authenticator.getUser().getUserName() + ") unsign of " + user.getUserName());
        user.removeEvent(selectedEvent);
        DataManager dataManager = new DataManager();
        dataManager.saveUser(user);
        return "";
    }

    public String signUser() {
        if (selectedEvent.getSelectedUser() != null) {
            System.out.println("Admin(" + authenticator.getUser().getUserName() + ") sign of " + selectedEvent.getSelectedUser().getUserName());
            selectedEvent.getSelectedUser().addEvent(selectedEvent);
            DataManager dataManager = new DataManager();
            dataManager.saveUser(selectedEvent.getSelectedUser());
        } else {
            System.out.println("Admin(" + authenticator.getUser().getUserName() + ") tried signing a user (failed, no user selected)");
        }
        return "";
    }

    public String signUserForActivity(Activity activity) {
        User selectedUser = selectedEvent.getSelectedUser();
        if (selectedUser != null) {
            System.out.println("Admin(" + authenticator.getUser().getUserName() + ") sign of " + selectedUser.getUserName() + " for " + activity.getName());
            if (selectedUser.getActivities().contains(activity)) {
                System.out.println("User was already attending activity");
            } else {
                selectedUser.addActivity(activity);
            }
            DataManager dataManager = new DataManager();
            dataManager.saveUser(selectedUser);
        } else {
            System.out.println("Admin(" + authenticator.getUser().getUserName() + ") tried signing a user for " + activity.getName() + " (failed, no user selected)");
        }

        return "";
    }

    public String unsignActivity(User user, Activity activity) {
        return "";
    }


    public Boolean hasUserPartnerRequest(User user) {
        DataManager dataManager = new DataManager();
        EventPartnerRequest eventPartnerRequest = dataManager.getEventPartnerRequest(user, selectedEvent);
        if (eventPartnerRequest != null) {
            return eventPartnerRequest.isPartnerRequest();
        }
        return false;
    }

    public Integer hasUserVegetarianRequest(User user) {
        DataManager dataManager = new DataManager();
        EventVegetarianRequest vegetarianRequest = dataManager.getEventVegetarianRequest(user, selectedEvent);
        if (vegetarianRequest != null) {
            return vegetarianRequest.getVegetarian();
        }
        return 0;
    }
}
