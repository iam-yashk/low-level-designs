package org.example.LLD.ParkingLot;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
// 1. Enums and Vehicle Models
enum VehicleType {
    CAR, BIKE, TRUCK
}
enum ParkingSpotType {
    COMPACT, LARGE, BIKE
}

// 2. Abstract Vehicle Class
abstract class Vehicle {
    protected String licensePlate;
    protected VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    public String getLicenseNumber() {
        return licensePlate;
    }
    public VehicleType getType() {
        return type;
    }
}

class Car extends Vehicle {
    public Car(String plate) {
        super(plate, VehicleType.CAR);
    }
}

class Bike extends Vehicle {
    public Bike(String plate) {
        super(plate, VehicleType.BIKE);
    }
}

class Truck extends Vehicle {
    public Truck(String plate) {
        super(plate, VehicleType.TRUCK);
    }
}

// 3. Parking Spot Class
abstract class ParkingSpot {
    private final String spotId;
    private boolean isOccupied;
    private Vehicle vehicle;
    private final int distanceFromGate;
    private final ParkingSpotType type;
    private final int floorNumber;

    public ParkingSpot(String spotId, int distanceFromGate, ParkingSpotType type, int floorNumber) {
        this.spotId = spotId;
        this.distanceFromGate = distanceFromGate;
        this.type = type;
        this.floorNumber = floorNumber;
    }

    public synchronized boolean isAvailable() {
        return !isOccupied;
    }

    public synchronized boolean assignVehicle(Vehicle vehicle) {
        if(isOccupied) return false;
        this.isOccupied = true;
        this.vehicle = vehicle;
        return true;
    }

    public synchronized void removeVehicle() {
        this.isOccupied = false;
        this.vehicle = null;
    }

    public Vehicle getVehicle() { return vehicle; }
    public String getSpotId() { return spotId; }
    public int getInstanceFromGate() { return distanceFromGate; }
    public ParkingSpotType getType() { return type; }
    public boolean isOccupied() { return isOccupied; }
    public int getFloorNumber() { return floorNumber; }

    public abstract boolean canFitVehicle(Vehicle vehicle);
}

class CompactSpot extends ParkingSpot {
    public CompactSpot(String id, int dist, int floor) {
        super(id, dist, ParkingSpotType.COMPACT, floor);
    }
    public boolean canFitVehicle(Vehicle vehicle) {
        return vehicle.getType() == VehicleType.CAR;
    }
}

class LargeSpot extends ParkingSpot {
    public LargeSpot(String id, int dist, int floor) {
        super(id, dist, ParkingSpotType.LARGE, floor);
    }
    public boolean canFitVehicle(Vehicle vehicle) {
        return vehicle.getType() == VehicleType.TRUCK;
    }
}

class BikeSpot extends ParkingSpot {
    public BikeSpot(String id, int dist, int floor) {
        super(id, dist, ParkingSpotType.BIKE, floor);
    }
    public boolean canFitVehicle(Vehicle vehicle) {
        return vehicle.getType() == VehicleType.BIKE;
    }
}

// 4. ParkingSpotFactory Class
class ParkingSpotFactory {
    public static ParkingSpot createSpot(ParkingSpotType type ,String id, int dist, int floor) {
        return switch(type) {
            case COMPACT -> new CompactSpot(id, dist, floor);
            case LARGE -> new LargeSpot(id, dist, floor);
            case BIKE -> new BikeSpot(id, dist, floor);
        };
    }
}

// 5. ParkingTicket Class
class ParkingTicket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final long entryTimestamp;
    private long exitTimestamp;

    public ParkingTicket(Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = UUID.randomUUID().toString(); // Universally Unique Identifier (UUID)
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTimestamp = new Date().getTime();
    }

    public String getTicketId() {
        return ticketId;
    }
    public Vehicle getVehicle() {
        return vehicle;
    }
    public ParkingSpot getSpot() {
        return spot;
    }
    public long getEntryTimestamp() {
        return entryTimestamp;
    }
    public long getExitTimestamp() {
        return exitTimestamp;
    }
    public void setExitTimestamp() {
        this.exitTimestamp = new Date().getTime();
    }
}

// 6. Observer (OPTIONAL)
interface ParkingLotObserver {
    void onVehicleParked(Vehicle vehicle, ParkingSpot spot);
    void onVehicleUnparked(Vehicle vehicle, ParkingSpot spot);
}

class ConsoleLogger implements ParkingLotObserver {
    public void onVehicleParked(Vehicle vehicle, ParkingSpot spot) {
        System.out.println("Vehicle " + vehicle.getLicenseNumber() + " parket at " + spot.getSpotId());
    }
    public void onVehicleUnparked(Vehicle vehicle, ParkingSpot spot) {
        System.out.println("Vehicle " + vehicle.getLicenseNumber() + " left spot " + spot.getSpotId());
    }
}

// 7. inteface FeeStrategy
interface FeeStrategy {
    double calculateFee(ParkingTicket ticket);
}

class HourlyBasedFeeStrategy implements FeeStrategy {
    @Override
    public double calculateFee(ParkingTicket ticket) {
        long duration = ticket.getExitTimestamp() - ticket.getEntryTimestamp();
        long hours = ((duration + (1000 * 60 * 60) - 1) / (1000 * 60 * 60));

        VehicleType type = ticket.getVehicle().getType();
        return switch(type) {
            case CAR -> hours * 20.0;
            case BIKE -> hours * 10.0;
            case TRUCK -> hours * 30.0;
        };
    }
}

class MinuteBasedFeeStrategy implements FeeStrategy {
    @Override
    public double calculateFee(ParkingTicket ticket) {
        long duration = ticket.getExitTimestamp() - ticket.getEntryTimestamp();
        long hours = ((duration + (1000 * 60) - 1) / (1000 * 60));

        VehicleType type = ticket.getVehicle().getType();
        return switch(type) {
            case CAR -> hours * 0.5;
            case BIKE -> hours * 0.2;
            case TRUCK -> hours * 1.0;
        };
    }
}


// 8. ParkingFloor Class
class ParkingFloor {
    private final int floorNumber;
    private final PriorityQueue<ParkingSpot> availableSpots;
    private final List<ParkingSpot> allSpots;

    public ParkingFloor(int floorNumber, List<ParkingSpot> allSpots) {
        this.floorNumber = floorNumber;
        this.allSpots = allSpots;
        this.availableSpots = new PriorityQueue<>(Comparator.comparingInt(ParkingSpot::getInstanceFromGate));
        this.availableSpots.addAll(allSpots);
    }

    public synchronized Optional<ParkingSpot> getAvailableSpot(Vehicle vehicle, Map<VehicleType, Set<ParkingSpotType>> allowedTypes) {
        List<ParkingSpot> buffer = new ArrayList<>();
        ParkingSpot selectedSpot = null;
        while(!availableSpots.isEmpty()) {
            ParkingSpot spot = availableSpots.poll();
            if(spot.isAvailable() && allowedTypes.get(vehicle.getType()).contains(spot.getType()) && spot.canFitVehicle(vehicle)) {
                selectedSpot = spot;
                break;
            }
            buffer.add(spot);
        }
        availableSpots.addAll(buffer);
        return Optional.ofNullable(selectedSpot);
    }

    public synchronized void releaseSpot(ParkingSpot spot) {
        availableSpots.offer(spot);
    }

    public int getFloorNumber() {
        return floorNumber;
    }
}

// 9. ParkingManager Class
class ParkingManager {
    private final ParkingLot lot = ParkingLot.getInstance();

    public ParkingTicket park(Vehicle vehicle) throws Exception {
        return lot.parkVehicle(vehicle);
    }
    public double unpark(String license) throws Exception {
        return lot.unparkVehicle(license);
    }
    public void registerObserver(ParkingLotObserver observer) {
        lot.addObserver(observer);
    }
    public void configure(List<ParkingFloor> floors, FeeStrategy strategy) {
        for(ParkingFloor floor: floors) lot.addFloor(floor);
        lot.setFeeStrategy(strategy);
    }
}

// 10. ParkingLot Class
class ParkingLot {
    private static final ParkingLot INSTANCE = new ParkingLot();
    private final List<ParkingFloor> floors = new ArrayList<>();
    private final Map<String, ParkingTicket> activeTickets = new ConcurrentHashMap<>();
    private final List<ParkingLotObserver> observers = new ArrayList<>();
    private FeeStrategy feeStrategy = new HourlyBasedFeeStrategy();

    private final Map<VehicleType, Set<ParkingSpotType>> allowedTypes = Map.of(
            VehicleType.CAR, Set.of(ParkingSpotType.COMPACT, ParkingSpotType.LARGE),
            VehicleType.BIKE, Set.of(ParkingSpotType.BIKE),
            VehicleType.TRUCK, Set.of(ParkingSpotType.LARGE)
    );

    private ParkingLot() {};

    public static ParkingLot getInstance() {
        return INSTANCE;
    }
    public void addObserver(ParkingLotObserver obs) {
        observers.add(obs);
    }
    public void addFloor(ParkingFloor floor) {
        floors.add(floor);
    }
    public void setFeeStrategy(FeeStrategy strategy) {
        this.feeStrategy = strategy;
    }
    public synchronized ParkingTicket parkVehicle(Vehicle vehicle) throws Exception {
        for(ParkingFloor floor: floors) {
            Optional<ParkingSpot> opt = floor.getAvailableSpot(vehicle, allowedTypes);
            if(opt.isPresent()) {
                ParkingSpot spot = opt.get();
                if(spot.assignVehicle(vehicle)) {
                    ParkingTicket ticket = new ParkingTicket(vehicle, spot);
                    activeTickets.put(vehicle.getLicenseNumber(), ticket);
                    observers.forEach(obs -> obs.onVehicleParked(vehicle, spot));
                    return ticket;
                }
            }
        }
        throw new Exception("No spot available for vehicle type " + vehicle.getType());
    }
    public synchronized double unparkVehicle(String license) throws Exception {
        ParkingTicket ticket = activeTickets.remove(license);
        if(ticket == null) throw new Exception("Ticket not found for vehicle " + license);

        ParkingSpot spot = ticket.getSpot();
        spot.removeVehicle();

        for(ParkingFloor floor: floors) {
            if(floor.getFloorNumber() == spot.getFloorNumber()) {
                floor.releaseSpot(spot);
                break;
            }
        }
        ticket.setExitTimestamp();
        observers.forEach(o -> o.onVehicleUnparked(ticket.getVehicle(), spot));
        return feeStrategy.calculateFee(ticket);
    }
}

// 11. ParkingLot Runner
public class ParkingLotMain {
    public static void main(String[] args) {
        ParkingManager manager = new ParkingManager();
        manager.registerObserver(new ConsoleLogger());

        ParkingFloor floor1 = new ParkingFloor(1, List.of(
            ParkingSpotFactory.createSpot(ParkingSpotType.BIKE, "F1-B1", 1, 1),
            ParkingSpotFactory.createSpot(ParkingSpotType.COMPACT, "F1-C1", 2, 1),
            ParkingSpotFactory.createSpot(ParkingSpotType.LARGE, "F1-L1", 3, 1)
        ));

        manager.configure(List.of(floor1), new HourlyBasedFeeStrategy());

        Vehicle car = new Car("CAR123");
        Vehicle bike = new Bike("BIKE123");

        try {
            ParkingTicket t1 = manager.park(car);
            ParkingTicket t2 = manager.park(bike);

            Thread.sleep(1000);

            System.out.println("Fee: " + manager.unpark(car.getLicenseNumber()));
            System.out.println("Fee: " + manager.unpark(bike.getLicenseNumber()));
        } catch (Exception e) {
            System.out.println("Error :" + e.getMessage());
        }
    }
}
