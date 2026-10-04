package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IAddressService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final IAddressService addressService;

    @Autowired
    public AddressController(IAddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * The owner is always the authenticated caller. This endpoint used to accept a userId in the
     * body, so anyone could file an address under another account and then read it back.
     */
    @PostMapping
    public ResponseEntity<Address> create(@RequestBody Address address, Authentication authentication) {
        Address created = addressService.create(address, CurrentCaller.id(authentication));
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Returns the caller's address book. The former "/user/{userId}" route is gone: the id in the
     * path could be anyone, so the list was a directory of every address in the system.
     */
    @GetMapping
    public ResponseEntity<List<Address>> getAll(Authentication authentication) {
        return ResponseEntity.ok(addressService.getByUser(CurrentCaller.id(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Address> read(@PathVariable UUID id, Authentication authentication) {
        Address address = addressService.read(id, CurrentCaller.id(authentication));
        if (address == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(address);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Address> update(@PathVariable UUID id,
                                          @RequestBody Address address,
                                          Authentication authentication) {
        Address toUpdate = new Address.Builder().copy(address).setId(id).build();
        Address updated = addressService.update(toUpdate, CurrentCaller.id(authentication));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        if (!addressService.delete(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /** The caller's default address. The former "/user/{userId}/default" route is gone. */
    @GetMapping("/default")
    public ResponseEntity<Address> getDefault(Authentication authentication) {
        Address address = addressService.getDefaultForUser(CurrentCaller.id(authentication));
        if (address == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(address);
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<Address> setDefault(@PathVariable UUID id, Authentication authentication) {
        Address address = addressService.setDefault(id, CurrentCaller.id(authentication));
        if (address == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(address);
    }
}