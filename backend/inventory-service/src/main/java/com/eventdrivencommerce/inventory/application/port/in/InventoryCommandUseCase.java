package com.eventdrivencommerce.inventory.application.port.in;

import com.eventdrivencommerce.inventory.application.model.AdjustStockCommand;
import com.eventdrivencommerce.inventory.application.model.ChangeReservationCommand;
import com.eventdrivencommerce.inventory.application.model.ReceiveStockCommand;
import com.eventdrivencommerce.inventory.application.model.ReservationResult;
import com.eventdrivencommerce.inventory.application.model.ReserveStockCommand;
import com.eventdrivencommerce.inventory.application.model.ReserveStockResult;
import com.eventdrivencommerce.inventory.application.model.StockResult;

public interface InventoryCommandUseCase {

    StockResult receive(ReceiveStockCommand command);

    StockResult adjust(AdjustStockCommand command);

    ReserveStockResult reserve(ReserveStockCommand command);

    ReservationResult release(ChangeReservationCommand command);

    ReservationResult complete(ChangeReservationCommand command);
}
