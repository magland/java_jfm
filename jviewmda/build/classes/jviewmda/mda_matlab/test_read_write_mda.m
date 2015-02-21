function test_read_write_mda

N1=20;
N2=30;
N3=10;

disp('...................................................');
disp('test 1');
X=rand(N1,N2,N3);
writemda(X,'tmp1.mda');
fprintf('%d should be close to %d\n',get_file_size('tmp1.mda'),length(X(:))*4);
X2=readmda('tmp1.mda');
fprintf('%f should be very small\n',max(abs(X(:)-X2(:))));
fprintf('These should match:\n'); disp(size(X)); disp(size(X2));

disp('...................................................');
disp('test 2');
X=floor(rand(N1,N2,N3)*255);
writemda(X,'tmp2.mda');
fprintf('%d should be close to %d\n',get_file_size('tmp2.mda'),length(X(:))*1);
X2=readmda('tmp2.mda');
fprintf('%f should be very small\n',max(abs(X(:)-X2(:))));
fprintf('These should match:\n'); disp(size(X)); disp(size(X2));

disp('...................................................');
disp('test 3');
X=floor(rand(N1,N2,N3)*10000);
writemda(X,'tmp3.mda');
fprintf('%d should be close to %d\n',get_file_size('tmp3.mda'),length(X(:))*2);
X2=readmda('tmp3.mda');
fprintf('%f should be very small\n',max(abs(X(:)-X2(:))));
fprintf('These should match:\n'); disp(size(X)); disp(size(X2));

disp('...................................................');
disp('test 4');
X=floor(rand(N1,N2,N3)*10000000);
writemda(X,'tmp4.mda');
fprintf('%d should be close to %d\n',get_file_size('tmp4.mda'),length(X(:))*4);
X2=readmda('tmp4.mda');
fprintf('%f should be very small\n',max(abs(X(:)-X2(:))));
fprintf('These should match:\n'); disp(size(X)); disp(size(X2));

disp('...................................................');
disp('test 5');
X=floor((2*rand(N1,N2,N3)-1)*100);
writemda(X,'tmp5.mda');
fprintf('%d should be close to %d\n',get_file_size('tmp5.mda'),length(X(:))*2);
X2=readmda('tmp5.mda');
fprintf('%f should be very small\n',max(abs(X(:)-X2(:))));
fprintf('These should match:\n'); disp(size(X)); disp(size(X2));

disp('...................................................');
disp('test 6');
X=floor((2*rand(N1,N2,N3)-1)*10000000);
writemda(X,'tmp6.mda');
fprintf('%d should be close to %d\n',get_file_size('tmp6.mda'),length(X(:))*4);
X2=readmda('tmp6.mda');
fprintf('%f should be very small\n',max(abs(X(:)-X2(:))));
fprintf('These should match:\n'); disp(size(X)); disp(size(X2));

disp('...................................................');
disp('test 7');
X=rand(N1,N2,N3)+i*rand(N1,N2,N3);
writemda(X,'tmp7.mda');
fprintf('%d should be close to %d\n',get_file_size('tmp7.mda'),length(X(:))*8);
X2=readmda('tmp7.mda');
fprintf('%f should be very small\n',max(abs(X(:)-X2(:))));
fprintf('These should match:\n'); disp(size(X)); disp(size(X2));

end

function ret=get_file_size(path)

s=dir(path);
ret=s.bytes;

end
