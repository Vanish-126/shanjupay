package com.shanjupay.user.convert;

import com.shanjupay.user.api.dto.tenant.AccountDTO;
import com.shanjupay.user.entity.Account;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2021-07-07T15:13:05+0800",
    comments = "version: 1.3.0.Final, compiler: javac, environment: Java 1.8.0_291 (Oracle Corporation)"
)
public class AccountConvertImpl implements AccountConvert {

    @Override
    public AccountDTO entity2dto(Account entity) {
        if ( entity == null ) {
            return null;
        }

        AccountDTO accountDTO = new AccountDTO();

        accountDTO.setUsername( entity.getUsername() );
        accountDTO.setId( entity.getId() );
        accountDTO.setMobile( entity.getMobile() );
        accountDTO.setPassword( entity.getPassword() );
        accountDTO.setSalt( entity.getSalt() );

        return accountDTO;
    }

    @Override
    public Account dto2entity(AccountDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Account account = new Account();

        account.setMobile( dto.getMobile() );
        account.setPassword( dto.getPassword() );
        account.setSalt( dto.getSalt() );
        account.setUsername( dto.getUsername() );
        account.setId( dto.getId() );

        return account;
    }
}
